package npl;

import jason.RevisionFailedException;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.LogicalFormula;
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

import npl.DeonticModality.State;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 
 * Interprets a NP for a particular scope 
 * 
 * @author jomi
 */
public class NPLInterpreter implements ToDOM {

    private Agent            ag = null; // use a Jason agent to store the facts (BB)
    private Map<String,Norm> regimentedNorms = null; // norms with failure consequence
    private Map<String,Norm> regulativeNorms  = null; // norms with obligation, permission, prohibition consequence

    private Object           syncTransState = new Object();
    
    List<NormativeListener>  listeners = new CopyOnWriteArrayList<NormativeListener>();
    
    private ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1); // a thread that checks deadlines of obligations, permissions, ...
    private StateTransitions   oblUpdateThread;
    
    public final static Atom NPAtom   = new Atom("np");
    public final static Atom NormAtom = new Atom("npli");

    public final static PredicateIndicator ACTPI  = new PredicateIndicator(State.active.name(),1);
    public final static PredicateIndicator FFPI   = new PredicateIndicator(State.fulfilled.name(),1);
    public final static PredicateIndicator UFPI   = new PredicateIndicator(State.unfulfilled.name(),1);
    public final static PredicateIndicator INACPI = new PredicateIndicator(State.inactive.name(),1);
    
    public void init() {
        ag              = new Agent();
        regimentedNorms = new HashMap<String,Norm>();
        regulativeNorms = new HashMap<String,Norm>();
        ag.initAg();
        clearFacts();
        oblUpdateThread = new StateTransitions();
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
    
    /** resets the interpreter with a new NP */
    public void setScope(Scope scope) {
        init();
        loadNP(scope);
    }
        
    /** loads facts from a NP scope into the normative state */
    public void loadNP(Scope scope) {
        if (ag == null) {
            init();
        }
        BeliefBase bb = ag.getBB();
        
        for (Rule r: scope.getRules()) {
            // normalise rules with empty body
            Literal l;
            if (r.getBody().equals(Literal.LTrue) && r.getHead().isGround())
                l = r.headClone();
            else
                l = r.clone();
            l.addSource(NPAtom);
            bb.add(1,l); // add in the end of the BB to preserve the program order
        }
        for (Norm n: scope.getNorms()) {
            if (n.getConsequence().getFunctor().equals(NormativeProgram.FailFunctor))
                regimentedNorms.put(n.getId(), n.clone());
            else 
                regulativeNorms.put(n.getId(), n.clone());
        }
        
        if (scope.getFather() != null)
            loadNP(scope.getFather());
    }
    
    /** removes all facts/rules of the normative state */
    public void clearFacts() {
        ag.getBB().clear();
        if (oblUpdateThread != null) oblUpdateThread.update();
    }
    
    /** removes a fact from the normative state */
    public boolean removeFact(Literal l) {
        if (!l.hasSource())
            l.addSource(NPAtom);
        try {
            return ag.delBel(l);
        } catch (RevisionFailedException e) {
            return false;
        } finally {
            // better to use explicit "verifyNorms" to trigger the update
            // if (oblUpdateThread != null) oblUpdateThread.update();            
        }
    }
    
    /** adds a fact into the normative state */
    public void addFact(Literal l) {
        if (!l.hasSource())
            l.addSource(NPAtom);
        try {
            ag.addBel(l);
            // better to use explicit "verifyNorms" to trigger the update
            //if (oblUpdateThread != null) oblUpdateThread.update();
        } catch (RevisionFailedException e) {
        }
    }
    

    /** get active obligations, permissions and prohibitions  */
    public List<DeonticModality> getActive() { return getByState(ACTPI, null); }
    public List<DeonticModality> getFulfilled() { return getByState(FFPI, null);  }
    public List<DeonticModality> getUnFulfilled() { return getByState(UFPI, null); }
    public List<DeonticModality> getInactive() { return getByState(INACPI, null); }
    
    /** get active obligations (those not fulfilled) */
    public List<DeonticModality> getActiveObligations() { return getByState(ACTPI, NormativeProgram.OblFunctor); }

    /** get fulfilled obligations */
    public List<DeonticModality> getFulfilledObligations() { return getByState(FFPI, NormativeProgram.OblFunctor);  }
    
    /** get unfulfilled obligations */
    public List<DeonticModality> getUnFulfilledObligations() { return getByState(UFPI, NormativeProgram.OblFunctor); }
    
    /** get fulfilled obligations */
    public List<DeonticModality> getInactiveObligations() { return getByState(INACPI, NormativeProgram.OblFunctor); }
    
    public List<DeonticModality> getActivePermissions() { return getByState(ACTPI, NormativeProgram.PerFunctor); }

    public List<DeonticModality> getActiveProhibitions() { return getByState(ACTPI, NormativeProgram.ProFunctor); }
    public List<DeonticModality> getFulfilledProhibitions() { return getByState(FFPI, NormativeProgram.ProFunctor);  }
    public List<DeonticModality> getUnFulfilledProhibitions() { return getByState(UFPI, NormativeProgram.ProFunctor); }
    public List<DeonticModality> getInactiveProhibitions() { return getByState(INACPI, NormativeProgram.ProFunctor); }
    
    private List<DeonticModality> getByState(PredicateIndicator state, String kind) {
        List<DeonticModality> ol = new ArrayList<DeonticModality>();
        synchronized (syncTransState) {
            Iterator<Literal> i = ag.getBB().getCandidateBeliefs(state);
            if (i != null) {
                while (i.hasNext()) {
                    Literal b = i.next();
                    if (b.hasSource(NormAtom)) {
                        DeonticModality o = (DeonticModality)b.getTerm(0);
                        if (kind == null || o.getFunctor().equals(kind))
                            ol.add(o);
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
        Norm n = regulativeNorms.get(id);
        if (n != null)
            return n;
        else
            return regimentedNorms.get(id);
    }
    
    /** 
     * verifies all norms to identify failure (exception) or new obligations, permissions and prohibitions
     *  
     * @return list of new obligations, permissions or prohibitions
     */
    public Collection<DeonticModality> verifyNorms() throws NormativeFailureException {
        BeliefBase bb = ag.getBB();
        List<DeonticModality> newObl = new ArrayList<DeonticModality>();
        synchronized (syncTransState) {            
            // test all fails first
            for (Norm n: regimentedNorms.values()) {
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
            
            List<DeonticModality> activeObl = getActive();
            
            // -- computes new obligations, permissions, and prohibitions
            for (Norm n: regulativeNorms.values()) {
                Iterator<Unifier> i = n.getCondition().logicalConsequence(ag, new Unifier());
                while (i.hasNext()) {
                    Unifier u = i.next();
                    DeonticModality obl = new DeonticModality((Literal)n.getConsequence(), u, n);
                    // check if already in BB
                    if (!containsIgnoreDeadline(activeObl, obl)) { // is it a new obligation?  
                        if ( (obl.isObligation() && !holds(obl.getAim())) || // that is an obligation not achieved yet
                              obl.isPermission()                          || // or a permission
                              obl.isProhibition()                            // or a prohibition
                           ) {
                            obl.setActive();
                            if (bb.add(createState(obl))) {
                                newObl.add(obl);
                                activeObl.add(obl);
                                addInSchedule(obl);
                                notifyCreated(obl);
                            }
                        }
                    }
                }
            }
            
        }
        oblUpdateThread.update();
        return newObl;
    }

    private void addInSchedule(final DeonticModality o) {
        long ttf = o.getDeadline() - System.currentTimeMillis();

        scheduler.schedule(new Runnable() {
            public void run() {
                oblUpdateThread.checkUnfulfilled(o);
            }
        }, ttf, TimeUnit.MILLISECONDS);
    }
    
    private void notifyCreated(DeonticModality o) {
        for (NormativeListener l: listeners)
            try {
                l.created(o.copy());                
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }
    private void notifyFulfilled(DeonticModality o) {
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
    private void notifyUnfulfilled(DeonticModality o) {
        for (NormativeListener l: listeners)
            try {
                l.unfulfilled(o.copy());
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }
    private void notifyInactive(DeonticModality o) {
        for (NormativeListener l: listeners)
            try {
                l.inactive(o.copy());
            } catch (Exception e) {
                System.err.println("Error notifying normative listener "+l);
                e.printStackTrace();
            }
    }

    
    private Literal createState(DeonticModality o) {
        Literal s = ASSyntax.createLiteral(o.getState().name(), o);
        s.addSource(NormAtom);
        return s;
    }
    
    private boolean containsIgnoreDeadline(Collection<DeonticModality> list, DeonticModality obl) {
        for (DeonticModality l: list)
            if (l.equalsIgnoreDeadline(obl))
                return true;
        return false;
    }
    
    public String getNormsString() {
        StringBuilder out = new StringBuilder();
        for (Norm n: regimentedNorms.values()) 
            out.append(n+".\n");
        for (Norm n: regulativeNorms.values()) 
            out.append(n+".\n");
        return out.toString();
    }
    
    public String getStateString() {
        StringBuilder out = new StringBuilder("--- normative state ---\n\n");
        out.append("active:\n");
        for (Literal l: getActive()) {
            out.append("  "+l+"\n");
        }
        out.append("\nfulfilled:\n");
        for (Literal l: getFulfilled()) {
            out.append("  "+l+"\n");
        }
        out.append("\nunfulfilled:\n");
        for (Literal l: getUnFulfilled()) {
            out.append("  "+l+"\n");
        }
        return out.toString();
    }
    
    public Element getAsDOM(Document document) {
        Element ele = (Element) document.createElement("normative-state");
        for (DeonticModality l: getUnFulfilled())
            ele.appendChild( obligation2dom(document, l, State.unfulfilled, true));
        for (DeonticModality l: getActive())
            ele.appendChild( obligation2dom(document, l, State.active, true));
        for (DeonticModality l: getFulfilled()) 
            ele.appendChild( obligation2dom(document, l, State.fulfilled, false));
        for (DeonticModality l: getInactive()) 
            ele.appendChild( obligation2dom(document, l, State.inactive, false));
        return ele;
    }
    private Element obligation2dom(Document document, DeonticModality l, State state, boolean reltime) {
        Element oblele = (Element) document.createElement("deontic-modality");
        try {            
            oblele.setAttribute("modality", l.getFunctor());
            oblele.setAttribute("state", state.name());
            oblele.setAttribute("agent", l.getAg().toString());
            if (l.maintContFromNorm)                 
                oblele.setAttribute("maintenance", "as in norm "+l.getNorm().getId());
            else
                oblele.setAttribute("maintenance", l.getMaitenanceCondition().toString());
            oblele.setAttribute("aim", l.getAim().toString());
            long ttf = l.getDeadline();
            if (reltime)
                oblele.setAttribute("ttf", TimeTerm.toRelTimeStr(ttf));
            else
                oblele.setAttribute("ttf", TimeTerm.toTimeStamp(ttf));
            
            String toff = l.getDoneStr();
            if (toff != null) {
                oblele.setAttribute("done", toff); //TimeTerm.toAbsTimeStr(toff));  
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
                            annotele.setAttribute("value", la.getTerms().toString());
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
        return "normative interpreter";
    }


    private int updateInterval = 1000;
    
    /** sets the update interval for checking the change in obligations' state */
    public void setUpdateInterval(int miliseconds) {
        updateInterval = miliseconds;
    }

    /** this thread updates the state of obligations, permissions and prohibitions (e.g. active -> fulfilled) 
        each second (by default) */
    class StateTransitions extends Thread {
        
        private boolean           update = false;
        private List<DeonticModality>  active = null;            
        private BeliefBase        bb;
        private Queue<DeonticModality> toCheckUnfulfilled = new ConcurrentLinkedQueue<DeonticModality>();
                        
        /** update the state of the obligations */
        synchronized void update() {
            update = true;
            notifyAll();
        }
        
        void setUpdateInterval(int miliseconds) {
            updateInterval = miliseconds;
        }
        
        synchronized void checkUnfulfilled(DeonticModality o) {
            toCheckUnfulfilled.offer(o);
            update = true;
            notifyAll();
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
                        wait(updateInterval);
                    }
                    if (update) {
                        bb = ag.getBB();
                        synchronized (syncTransState) {
                            update = false;
                            updateActive();
                            updateInactive();
                            updateDeadline();
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
        
        // -- transition active -> (un)fulfilled (based on aim)
        private void updateActive() {
            active = getActive();  
            for (DeonticModality o: active) {
                Literal oasinbb = createState(o);
                if (o.isObligation()) {
                    // transition active -> fulfilled
                    if (o.getAg().isGround() && holds(o.getAim())) {                                        
                        if (!bb.remove(oasinbb)) System.out.println("ooops "+oasinbb+" should be removed 2");
                        o = o.copy();
                        o.setFulfilled();
                        bb.add(createState(o));
                        notifyFulfilled(o);
                    } else {
                        List<DeonticModality> fuls = getFulfilledObligations();
                        Iterator<Unifier> i = o.getAim().logicalConsequence(ag, new Unifier());
                        while (i.hasNext()) {
                            Unifier u = i.next();
                            DeonticModality obl = new DeonticModality( new LiteralImpl(o), u, o.getNorm());
                            if (!containsIgnoreDeadline(fuls, obl)) {
                                o.incAgInstance();
                                obl.setFulfilled();
                                bb.add(createState(obl));
                                notifyFulfilled(obl);
                            }
                        }
                    }
                } else if (o.isProhibition()) {
                    // transition active -> unfulfilled
                    if (o.getAg().isGround() && holds(o.getAim())) { // the case of a prohibition for one agent
                        if (!bb.remove(oasinbb)) System.out.println("ooops "+oasinbb+" should be removed 2");
                        o = o.copy();
                        o.setUnfulfilled();
                        bb.add(createState(o));
                        notifyUnfulfilled(o);                        
                    } else {
                        List<DeonticModality> unfuls = getUnFulfilledProhibitions();
                        Iterator<Unifier> i = o.getAim().logicalConsequence(ag, new Unifier());
                        while (i.hasNext()) {
                            Unifier u = i.next();
                            DeonticModality obl = new DeonticModality( new LiteralImpl(o), u, o.getNorm());
                            if (!containsIgnoreDeadline(unfuls, obl)) {
                                o.incAgInstance();
                                obl.setUnfulfilled();
                                bb.add(createState(obl));
                                notifyUnfulfilled(obl);
                            }
                        }
                    }
                }
            }            
        }
        
        // -- transition active -> inactive (based on r)
        private void updateInactive() {
            active = getActive();            
            for (DeonticModality o: active) { 
                Literal oasinbb = createState(o);
                if (! o.getMaitenanceCondition().logicalConsequence(ag, new Unifier()).hasNext()) {
                    // transition active -> inactive
                    if (!bb.remove(oasinbb)) System.out.println("ooops "+oasinbb+" should be removed 1!");
                    o.setInactive();
                    notifyInactive(o);
                }
            }            
        }
        
        // -- transition active -> unfulfilled (for obl) (based on d)
        //               active -> inactive (for per)
        //               active -> fulfilled (for pro)
        private void updateDeadline() {
            active = getActive();            
            DeonticModality o = toCheckUnfulfilled.poll();
            while (o != null) {
                if (containsIgnoreDeadline(active, o)) { // deadline achieved, and still active
                    Literal oasinbb = createState(o);                
                    if (!bb.remove(oasinbb)) System.out.println("ooops 3 "+o+" should be removed (due the deadline), but it is not in the set of facts.");

                    if (o.isObligation()) {
                        // transition for prohibition (active -> unfulfilled)
                        if (o.getAgIntances() == 0) { // it is unfulfilled only if no agent instance has fulfilled the prohibition
                            o.setUnfulfilled();
                            bb.add(createState(o));
                            notifyUnfulfilled(o);
                        }
                    } else if (o.isPermission()) {
                        // transition for prohibition (active -> inactive)
                        o.setInactive();
                        bb.add(createState(o));
                        notifyInactive(o);
                    } else {
                        // transition for prohibition (active -> fulfilled)
                        if (o.getAgIntances() == 0) { // it is fulfilled only if no agent instance has unfulfilled the prohibition
                            o.setFulfilled();
                            bb.add(createState(o));
                            notifyFulfilled(o);
                        }
                    }
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
            List<DeonticModality> unfulObl  = getUnFulfilledObligations();
            List<DeonticModality> unfulPlusInactObls = getInactiveObligations();
            unfulPlusInactObls.addAll(unfulObl);
            for (DeonticModality o: unfulPlusInactObls) {
                if (holds(o.getAim()) && o.getAnnots("done").isEmpty()) { // if the agent did, even latter...
                    long ttf = System.currentTimeMillis()-o.getDeadline();
                    o.addAnnot(ASSyntax.createStructure("done", new TimeTerm(ttf, "milliseconds")));                
                }            
            }
        }
                
    }
}
