package npl;

import jason.NoValueException;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.PredicateIndicator;
import jason.asSyntax.Rule;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.bb.BeliefBase;
import jason.util.ToDOM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 
 * Interprets a NP for a particular scope 
 * 
 * @author jomi
 */
public class NPLInterpreter implements ToDOM {

    private Agent            ag = null; // use a Jason agent to store the facts (BB)
    private Map<String,Norm> normsFail = null; // norms with failure consequence
    private Map<String,Norm> normsObl  = null; // norms with obligation consequence
    private Scope            scope = null;

    private Object           syncTransState = new Object();
    
    List<NormativeListener>  listeners = new CopyOnWriteArrayList<NormativeListener>();
    
    private ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1); // a thread that checks deadlines of obligations
    private ObligationStateTransition   oblUpdateThread;
    
    public final static Atom NPAtom   = new Atom("np");
    public final static Atom DynAtom  = new Atom("dyn");
    public final static Atom NormAtom = new Atom("npli");

    public void init() {
        ag        = new Agent();
        normsFail = new HashMap<String,Norm>();
        normsObl  = new HashMap<String,Norm>();
        ag.initAg();
        clearNP();
        //clearDynamicFacts();    
        oblUpdateThread = new ObligationStateTransition();
        oblUpdateThread.start();
    }
    
    public void stop() {
        scheduler.shutdownNow();
        if (oblUpdateThread != null)
            oblUpdateThread.interrupt();
    }
    
    public void addListener(NormativeListener ol) {
        listeners.add(ol);
    }
    public boolean removeListener(NormativeListener ol) {
        return listeners.remove(ol);
    }
    
    /** get all facts from a kind of source (os or oe) */
    public List<Literal> getSource(Atom s) {
        List<Literal> oel = new ArrayList<Literal>();
        for (Literal b: ag.getBB())
            if (b.hasSource(s))
                oel.add(b);
        return oel;
    }

    
    /** resets the interpreter with a new NP */
    public void setScope(Scope scope) {
        init();
        this.scope = scope;
        loadNP(scope);
    }
    
    public Scope getScope() {
        return scope;
    }
    
    /** loads facts from a NP scope into the interpreter */
    public void loadNP(Scope scope) {
        BeliefBase bb = ag.getBB();
        
        for (Rule r: scope.getRules()) {
            // normalise rules with empty body
            Literal l;
            if (r.getBody().equals(Literal.LTrue) && r.isGround())
                l = r.headClone();
            else
                l = r.clone();
            l.addSource(NPAtom);
            bb.add(1,l); // add in the end of the BB to preserve the program order
        }
        for (Norm n: scope.getNorms()) {
            if (n.getConsequence().getFunctor().equals(NormativeProgram.OblFunctor))
                normsObl.put(n.getId(), n.clone());
            else 
                normsFail.put(n.getId(), n.clone());
        }
        
        if (scope.getFather() != null)
            loadNP(scope.getFather());
    }
    
    /** removes all facts/rules that comes from NP */
    public void clearNP() {
        BeliefBase bb = ag.getBB();
        for (Literal b: getSource(NPAtom))
            bb.remove(b);
    }
    
    
    /** get active obligations (those not fulfilled) */
    public List<Obligation> getActiveObligations() {
        return getObligationsByState(NormativeProgram.ACTPI);
    }

    /** get fulfilled obligations */
    public List<Obligation> getFulfilledObligations() {
        return getObligationsByState(NormativeProgram.FFPI);
    }
    
    /** get unfulfilled obligations */
    public List<Obligation> getUnFulfilledObligations() {
        return getObligationsByState(NormativeProgram.UFPI);
    }
    
    /** get fulfilled obligations */
    public List<Obligation> getInactiveObligations() {
        return getObligationsByState(NormativeProgram.INACPI);
    }
    
    private List<Obligation> getObligationsByState(PredicateIndicator state) {
        List<Obligation> ol = new ArrayList<Obligation>();
        synchronized (syncTransState) {
            Iterator<Literal> i = ag.getBB().getCandidateBeliefs(state);
            if (i != null) {
                while (i.hasNext()) {
                    Literal b = i.next();
                    if (b.hasSource(NormAtom)) {
                        ol.add((Obligation)b.getTerm(0));
                    }
                }
            }
        }
        return ol;
    }

    
    public Agent getAg() {
        return ag;
    }
    
    public boolean holds(LogicalFormula l) {
        try {
            Iterator<Unifier> i = l.logicalConsequence(ag, new Unifier());
            return i.hasNext();
        } catch (ConcurrentModificationException e) {
            System.out.println("*-*-* concurrent exception in NPLI holds method, I'll try again later....");
            // try again later
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {            }
            return holds(l);
        }
    }
    
    public Norm getNorm(String id) {
        Norm n = normsObl.get(id);
        if (n != null)
            return n;
        else
            return normsFail.get(id);
    }
    
    /** 
     * verifies all norms to identify failure (exception) or new obligations
     *  
     * @return list of obligations added
     */
    public Collection<Literal> verifyNorms() throws NormativeFailureException {
        BeliefBase bb = ag.getBB();
        List<Literal> newObl = new ArrayList<Literal>();
        synchronized (syncTransState) {            
            // test all fails first
            for (Norm n: normsFail.values()) {
                Iterator<Unifier> i = n.getCondition().logicalConsequence(ag, new Unifier());
                while (i.hasNext()) {
                    Unifier u = i.next();
                    //System.out.println("    solution "+u+" for "+n.getCondition());
                    Literal head = (Literal)n.getConsequence().capply(u);
                    if (head.getFunctor().equals(NormativeProgram.FailFunctor)) {
                        notifyNormFailure(head);
                        throw new NormativeFailureException((Structure)head);
                    }
                }
            }            
            
            List<Obligation> activeObl = getActiveObligations();
            
            // -- computes new obligations
            for (Norm n: normsObl.values()) {
                Iterator<Unifier> i = n.getCondition().logicalConsequence(ag, new Unifier());
                while (i.hasNext()) {
                    Unifier u = i.next();
                    //System.out.println("    solution "+u+" for "+n.getCondition());
                    Obligation obl = new Obligation((Literal)n.getConsequence().capply(u), n);
                    obl.restoreMaintenanceCondition(); // undo capply effects on maintenance condition
                    // check if already in BB
                    if (!containsIgnoreDeadline(activeObl, obl) // is it a new obligation?  
                        //!containsIgnoreDeadline(unfulObl, head) &&
                        //!containsIgnoreDeadline(fulObl, head)
                        //) {
                        && !holds(obl.getAim())) { // that is not achieved yet
                        
                        obl.addAnnot(ASSyntax.createStructure("created", new TimeTerm(0,null)));
                        if (bb.add(createObligationState(NormativeProgram.ActFunctor, obl))) {
                            //System.out.println("add "+createObligationState(NormativeProgram.ActFunctor, obl));
                            //System.out.println("* create "+obl+"\nactive: "+ activeObl);
                            newObl.add(obl);
                            activeObl.add(obl);
                            addObligationInSchedule(obl);
                            notifyOblCreated(obl);
                        }
                    }
                }
            }
            
        }
        oblUpdateThread.update();
        return newObl;
    }

    private long getOblTTF(final Literal o, final int pos) {
        try {
            return (long)((NumberTerm)o.getTerm(pos)).solve();
        } catch (NoValueException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    private void addObligationInSchedule(final Obligation o) {
        long ttf = o.getDeadline() - System.currentTimeMillis();

        scheduler.schedule(new Runnable() {
            public void run() {
                oblUpdateThread.checkUnfulfilled(o);
            }
        }, ttf, TimeUnit.MILLISECONDS);
    }
    
    private void notifyOblCreated(Obligation o) {
        for (NormativeListener l: listeners)
            try {
                l.created(o.copy());                
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }
    private void notifyOblFulfilled(Obligation o) {
        for (NormativeListener l: listeners)
            try {
                l.fulfilled(o.copy());
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }
    private void notifyNormFailure(Literal f) {
        for (NormativeListener l: listeners)
            try {
                l.failure((Structure)f.clone());
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }
    private void notifyOblUnfulfilled(Obligation o) {
        for (NormativeListener l: listeners)
            try {
                l.unfulfilled(o.copy());
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }
    private void notifyOblInactive(Obligation o) {
        for (NormativeListener l: listeners)
            try {
                l.inactive(o.copy());
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }

    
    private boolean activationConditionHolds(Obligation obl) {
        //Norm n = obl.getNorm(); // TODO: review
        // if the condition of the norm still holds
        Iterator<Unifier> i = obl.getMaitenanceCondition().logicalConsequence(ag, new Unifier());
        /*
        while (i.hasNext()) {
            Unifier u = i.next();
            Obligation head = new Obligation((Literal)n.getConsequence().capply(u), n);
            if (head.equalsIgnoreDeadline(obl)) {
                return true;
            }
        }
        return false;
        */
        return i.hasNext();
    }
    
    private Literal createObligationState(String state, Obligation o) {
        Literal s = ASSyntax.createLiteral(state, o);
        s.addSource(NormAtom);
        return s;
    }
    
    private boolean containsIgnoreDeadline(Collection<Obligation> list, Obligation obl) {
        for (Obligation l: list)
            if (l.equalsIgnoreDeadline(obl))
                return true;
        return false;
    }
    
    public String getStateString() {
        StringBuilder out = new StringBuilder("--- normative state for program "+scope.getId()+" ---\n\n");
        out.append("active obligations:\n");
        for (Literal l: getActiveObligations()) {
            out.append("  "+wellFormatTime(l)+"\n");
        }
        out.append("\nunfulfilled obligations:\n");
        for (Literal l: getUnFulfilledObligations()) {
            out.append("  "+wellFormatTime(l)+"\n");
        }
        out.append("\nfulfilled obligations:\n");
        for (Literal l: getFulfilledObligations()) {
            out.append("  "+wellFormatTime(l)+"\n");
        }
        return out.toString();
    }
    private String wellFormatTime(Literal l) {
        if (l.getFunctor().equals(NormativeProgram.OblFunctor)) {
            long t = ((Obligation)l).getDeadline();
            return l.getFunctor()+"("+l.getTerm(0)+","+l.getTerm(1)+","+l.getTerm(2)+","+TimeTerm.toRelTimeStr(t)+")";
        } else if (l.getFunctor().equals(NormativeProgram.FFFunctor)) {
            Obligation o = (Obligation)l.getTerm(0);
            long t = o.getDeadline();
            String so = o.getFunctor()+"("+o.getTerm(0)+","+o.getTerm(1)+","+o.getTerm(2)+","+TimeTerm.toTimeStamp(t)+")";
            t = getOblTTF(l,1);
            return l.getFunctor()+"("+so+","+TimeTerm.toAbsTimeStr(t)+")";            
        }
        return l.toString();
    }
    
    public Element getAsDOM(Document document) {
        Element ele = (Element) document.createElement("normative-state");
        if (scope != null)
            ele.setAttribute("id", scope.getId().toString());
        for (Obligation l: getUnFulfilledObligations())
            ele.appendChild( obligation2dom(document, l, "unfulfilled", true));
        for (Obligation l: getActiveObligations())
            ele.appendChild( obligation2dom(document, l, "active", true));
        for (Obligation l: getFulfilledObligations()) 
            ele.appendChild( obligation2dom(document, l, "fulfilled", false));
        for (Obligation l: getInactiveObligations()) 
            ele.appendChild( obligation2dom(document, l, "inactive", false));
        return ele;
    }
    private Element obligation2dom(Document document, Obligation l, String state, boolean reltime) {
        Element oblele = (Element) document.createElement("obligation");
        try {            
            oblele.setAttribute("state", state);
            oblele.setAttribute("agent", l.getAg().toString());
            oblele.setAttribute("maintenance", l.getMaitenanceCondition().toString());
            oblele.setAttribute("adim", l.getAim().toString());
            long ttf = l.getDeadline();
            if (reltime)
                oblele.setAttribute("ttf", TimeTerm.toRelTimeStr(ttf));
            else
                oblele.setAttribute("ttf", TimeTerm.toTimeStamp(ttf));
            
            List<Term> al = l.getAnnots("done");
            if (!al.isEmpty()) {
                Structure annot = (Structure)al.get(0);
                long toff = getOblTTF(annot,0);
                oblele.setAttribute("done", TimeTerm.toAbsTimeStr(toff));  
            }
        } catch (Exception e) {
            System.err.println("Error adding attribute in DOM for "+l+" "+state);
            e.printStackTrace();            
        }

        try {       
            if (l.hasAnnot()) {
                for (Term t: l.getAnnots()) {
                    if (t instanceof Literal) {
                        Literal la = (Literal)t;
                        if (!la.getFunctor().equals("done")) {
                            Element annotele = (Element) document.createElement("annotation");
                            annotele.setAttribute("id", la.getFunctor());
                            if (la.getArity() == 1 && la.getTerm(0) instanceof TimeTerm) {
                                annotele.setAttribute("value", TimeTerm.toTimeStamp( (long)((TimeTerm)la.getTerm(0)).solve() ));                    
                            } else {
                                annotele.setAttribute("value", la.getTerms().toString());
                            }
                            oblele.appendChild(annotele);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error adding annotations in DOM for "+l+" "+state);
            e.printStackTrace();
        }
        return oblele;
    }

    
    @Override
    public String toString() {
        return "normative interpreter for "+scope.getId();
    }


    private int updateInterval = 1000;
    
    /** sets the update interval for checking the change in obligation states */
    public void setUpdateInterval(int miliseconds) {
        updateInterval = miliseconds;
    }

    /** this thread updates the state of obligations (e.g. active -> fulfilled) 
        each second (by default) */
    class ObligationStateTransition extends Thread {
        
        private boolean           update = false;
        private List<Obligation>  activeObl = null;            
        private BeliefBase        bb;
        private Queue<Obligation> toCheckUnfulfilled = new ConcurrentLinkedQueue<Obligation>();
                        
        /** update the state of the obligations */
        void update() {
            update = true;
        }
        
        void setUpdateInterval(int miliseconds) {
            updateInterval = miliseconds;
        }
        
        void checkUnfulfilled(Obligation o) {
            toCheckUnfulfilled.offer(o);
            update = true;
        }
        
        @Override
        synchronized public void run() {
            boolean concModifExp = false;
            while (true) {
                try {
                    if (concModifExp) {
                        sleep(50);
                        concModifExp = false;
                    } else {
                        sleep(updateInterval);
                    }
                    if (update) {
                        bb = ag.getBB();
                        synchronized (syncTransState) {
                            update = false;
                            updateActive();
                            updateUnfulfilled();
                            updateDoneForUnfulfilled();
                        }
                    }
                } catch (ConcurrentModificationException e) {
                    // sleeps a while and try again
                    concModifExp = true;
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        // -- transition active -> inactive, fulfilled
        private void updateActive() {
            activeObl = getActiveObligations();  
            for (Obligation o: activeObl) { 
                Literal oasinbb = createObligationState(NormativeProgram.ActFunctor, o);
                boolean done = holds(o.getAim());
                long ttf = System.currentTimeMillis()-o.getDeadline();
                if (done) {
                    // transition active -> fulfilled
                    if (!bb.remove(oasinbb)) System.out.println("ooops obligation should be removed 2");
                    o = o.copy();
                    o.addAnnot(ASSyntax.createStructure("done", new TimeTerm(ttf, "milliseconds")));
                    o.addAnnot(ASSyntax.createStructure("fulfilled", new TimeTerm(0,null)));
                    //System.out.println("fulfilled "+o);
                    bb.add(createObligationState(NormativeProgram.FFFunctor, o));
                    notifyOblFulfilled(o);
                } else if (!activationConditionHolds(o)) {
                    // transition active -> inactive
                    if (!bb.remove(oasinbb)) System.out.println("ooops obligation should be removed 1");
                    o.addAnnot(ASSyntax.createStructure("inactive", new TimeTerm(0,null)));
                    if (!bb.add(createObligationState(NormativeProgram.InactFunctor, o))) System.out.println("ooops inactive obligation should be added");
                    notifyOblInactive(o);
                }
            }            
            activeObl = getActiveObligations();            
        }
        
        // -- transition active -> unfulfilled
        private void updateUnfulfilled() {
            Obligation o = toCheckUnfulfilled.poll();
            while (o != null) {
                if (containsIgnoreDeadline(activeObl, o)) {
                    //System.out.println("*** unfulfilled "+o);
                    Literal oasinbb = createObligationState(NormativeProgram.ActFunctor, o);                
                    if (!bb.remove(oasinbb)) System.out.println("ooops 3 obligation "+o+" should be removed, becomes unfulfilled, but it is not in the set of facts.");
                    o.addAnnot(ASSyntax.createStructure("unfulfilled", new TimeTerm(0,null)));
                    bb.add(createObligationState(NormativeProgram.UFFFunctor, o));
                    notifyOblUnfulfilled(o);
                    try {
                        verifyNorms();
                    } catch (NormativeFailureException e) {
                        //System.err.println("Error to set obligation "+o+" to unfulfilled!");
                        //e.printStackTrace();
                    }
                }
                o = toCheckUnfulfilled.poll();
            }
        }
        
        private void updateDoneForUnfulfilled() {
            // check done for unfulfilled and inactive
            List<Obligation> unfulObl  = getUnFulfilledObligations();
            List<Obligation> unfulPlusInactObls = getInactiveObligations();
            unfulPlusInactObls.addAll(unfulObl);
            for (Obligation o: unfulPlusInactObls) {
                if (holds(o.getAim()) && o.getAnnots("done").isEmpty()) { // if the agent did, even latter...
                    long ttf = System.currentTimeMillis()-o.getDeadline();
                    o.addAnnot(ASSyntax.createStructure("done", new TimeTerm(ttf, "milliseconds")));                
                }            
            }
        }
                
    }
}
