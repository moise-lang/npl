package npl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jason.asSyntax.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jason.RevisionFailedException;
import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.bb.BeliefBase;
import jason.util.ToDOM;
import npl.NormInstance.State;

/**
 * Interprets a NP for a particular scope
 *
 * @author jomi
 */
public class NPLInterpreter implements ToDOM, DynamicFactsProvider {

    private Agent ag = null; // use a Jason agent to store the facts (BB)
    private Map<String, INorm> regimentedNorms = null; // norms with failure consequence
    private Map<String, INorm> regulativeNorms = null; // norms with obligation, permission, prohibition consequence
    private Map<String, ISanctionRule> sanctionRules = null;

    protected Object syncTransState = new Object();

    List<NormativeListener> listeners = new CopyOnWriteArrayList<>();

    private ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1); // a thread that checks deadlines of obligations, permissions, ...
    private StateTransitions oblTransitions = null;

    protected Notifier notifier;

    protected Logger logger = Logger.getLogger(NPLInterpreter.class.getName());

    public final static Atom NPAtom = new Atom("np");
    public final static Atom NormAtom = new Atom("npli");

    public final static PredicateIndicator ACTPI = new PredicateIndicator(State.active.name(), 1);
    public final static PredicateIndicator FFPI = new PredicateIndicator(State.fulfilled.name(), 1);
    public final static PredicateIndicator UFPI = new PredicateIndicator(State.unfulfilled.name(), 1);
    public final static PredicateIndicator INACPI = new PredicateIndicator(State.inactive.name(), 1);
    public final static PredicateIndicator CSANCTION = new PredicateIndicator("sanction", 1);

    public void init() {
        ag = new Agent();
        regimentedNorms = new HashMap<>();
        regulativeNorms = new HashMap<>();
        sanctionRules   = new HashMap<>();
        ag.initAg();
        clearFacts();
        if (oblTransitions == null)
            setStateManager(new StateTransitionsThread(this,1000));

        notifier = new Notifier();
    }

    public void setStateManager(StateTransitions t) {
        if (oblTransitions != null)
            oblTransitions.stop();
        oblTransitions = t;
        oblTransitions.start();
    }

    public void stop() {
        scheduler.shutdownNow();
        if (oblTransitions != null)
            oblTransitions.stop();
    }

    public void addListener(NormativeListener ol) {
        listeners.add(ol);
    }

    public boolean removeListener(NormativeListener ol) {
        return listeners.remove(ol);
    }

    /**
     * loads facts from a NP scope into the normative state
     */
    public void loadNP(Scope scope) {
        loadNP(scope, true);
    }

    /**
     * loads facts from a NP scope into the normative state
     */
    public void loadNP(Scope scope, boolean autoIds) {
        if (ag == null)
            init();

        logger = Logger.getLogger(NPLInterpreter.class.getName() + "_" + scope.getId());

        var bb = ag.getBB();

        for (Rule r : scope.getInferenceRules()) {
            // normalise rules with empty body
            Literal l;
            if (r.getBody().equals(Literal.LTrue) && r.getHead().isGround())
                l = r.headClone();
            else
                l = r.clone();
            l.addSource(NPAtom);
            bb.add(1, l); // add in the end of the BB to preserve the program order
        }

        for (ISanctionRule sr : scope.getSanctionRules()) {
            sanctionRules.put(sr.getId(), sr.cloneSanction());
        }

        for (INorm n : scope.getNorms()) {
            // auto id management
            String id = n.getId();
            if (getNorm(id) != null) {
                if (autoIds) {
                    while (getNorm(id) != null)
                        id = id + id;
                } else {
                    logger.warning("Norm with id " + id + " already exists! It will be replaced by " + n);
                }
            }

            // add norm in the proper set
            addNorm(id, n);
        }

        if (scope.getFather() != null)
            loadNP(scope.getFather(), autoIds);
    }

    public void addNorm(INorm n) {
        addNorm(n.getId(), n);
    }

    public void addNorm(String id, INorm n) {
        if (n.getConsequence().getFunctor().equals(NormativeProgram.FailFunctor))
            regimentedNorms.put(id, n.clone());
        else
            regulativeNorms.put(id, n.clone());
    }


    /**
     * removes all facts/rules of the normative state
     */
    public void clearFacts() {
        ag.getBB().clear();
        if (oblTransitions != null)
            oblTransitions.update();
    }

    /**
     * removes a fact from the normative state
     */
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

    /**
     * adds a fact into the normative state
     */
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


    /**
     * get active obligations, permissions and prohibitions
     */
    public List<NormInstance> getActive() {
        return getByState(ACTPI, null);
    }

    public List<NormInstance> getFulfilled() {
        return getByState(FFPI, null);
    }

    public List<NormInstance> getUnFulfilled() {
        return getByState(UFPI, null);
    }

    public List<NormInstance> getInactive() {
        return getByState(INACPI, null);
    }

    /**
     * get active obligations (those not fulfilled)
     */
    public List<NormInstance> getActiveObligations() {
        return getByState(ACTPI, NormativeProgram.OblFunctor);
    }

    /**
     * get fulfilled obligations
     */
    public List<NormInstance> getFulfilledObligations() {
        return getByState(FFPI, NormativeProgram.OblFunctor);
    }

    /**
     * get unfulfilled obligations
     */
    public List<NormInstance> getUnFulfilledObligations() {
        return getByState(UFPI, NormativeProgram.OblFunctor);
    }

    /**
     * get fulfilled obligations
     */
    public List<NormInstance> getInactiveObligations() {
        return getByState(INACPI, NormativeProgram.OblFunctor);
    }

    public List<NormInstance> getActivePermissions() {
        return getByState(ACTPI, NormativeProgram.PerFunctor);
    }

    public List<NormInstance> getActiveProhibitions() {
        return getByState(ACTPI, NormativeProgram.ProFunctor);
    }

    public List<NormInstance> getFulfilledProhibitions() {
        return getByState(FFPI, NormativeProgram.ProFunctor);
    }

    public List<NormInstance> getUnFulfilledProhibitions() {
        return getByState(UFPI, NormativeProgram.ProFunctor);
    }

    public List<NormInstance> getInactiveProhibitions() {
        return getByState(INACPI, NormativeProgram.ProFunctor);
    }

    private List<NormInstance> getByState(PredicateIndicator state, String kind) {
        List<NormInstance> ol = new ArrayList<>();
        synchronized (syncTransState) {
            Iterator<Literal> i = ag.getBB().getCandidateBeliefs(state);
            if (i != null) {
                while (i.hasNext()) {
                    Literal b = i.next();
                    if (b.hasSource(NormAtom)) {
                        NormInstance o = (NormInstance) b.getTerm(0);
                        if (kind == null || o.getFunctor().equals(kind))
                            ol.add(o);
                    }
                }
            }
        }
        return ol;
    }

    private List<Literal> getCreatedSanctions() {
        var ol = new ArrayList<Literal>();
        synchronized (syncTransState) {
            Iterator<Literal> i = ag.getBB().getCandidateBeliefs(CSANCTION);
            if (i != null) {
                while (i.hasNext()) {
                    Literal b = i.next();
                    if (b.hasSource(NormAtom)) {
                        ol.add((Literal) b.getTerm(0));
                    }
                }
            }
        }
        return ol;
    }

    @Override
    public boolean isRelevant(PredicateIndicator pi) {
        return pi.equals(ACTPI) || pi.equals(FFPI) || pi.equals(UFPI) || pi.equals(INACPI)|| pi.equals(CSANCTION);
    }

    @Override
    public Iterator<Unifier> consult(Literal l, Unifier u) {
        List<Unifier> ol = new ArrayList<>(); // TODO: use an iterator instead of list, lazy approach
        synchronized (syncTransState) {
            Iterator<Literal> i = ag.getBB().getCandidateBeliefs(l, u);
            if (i != null) {
                while (i.hasNext()) {
                    Unifier un = u.clone();
                    if (un.unifies(l, i.next())) {
                        ol.add(un);
                    }
                }
            }
        }
        return ol.iterator();
    }

    protected Agent getAg() {
        return ag;
    }

    public List<Literal> getFacts() {
        var r = new ArrayList<Literal>(getAg().getBB().size() + 5);
        for (var l : getAg().getBB()) {
            r.add(l);
        }
        return r;
    }

    public boolean holds(LogicalFormula l) {
        try {
            Iterator<Unifier> i = l.logicalConsequence(ag, new Unifier());
            return i.hasNext();
        } catch (ConcurrentModificationException e) {
            logger.log(Level.FINE, "*-*-* concurrent exception in NPLI holds method, I'll try again later....");
            // try again later
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
            }
            return holds(l);
        }
    }

    public Iterator<Unifier> solve(LogicalFormula l) {
        try {
            return l.logicalConsequence(ag, new Unifier());
        } catch (ConcurrentModificationException e) {
            logger.log(Level.FINE, "*-*-* concurrent exception in NPLI holds method, I'll try again later....");
            // try again later
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
            }
            return solve(l);
        }
    }

    public INorm getNorm(String id) {
        INorm n = regulativeNorms.get(id);
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
    public Collection<NormInstance> verifyNorms() throws NormativeFailureException {
        List<NormInstance> newObl = new ArrayList<>();
        synchronized (syncTransState) {
            // test all fails first
            for (INorm n : regimentedNorms.values()) {
                Iterator<Unifier> i = n.getCondition().logicalConsequence(ag, new Unifier());
                while (i.hasNext()) {
                    Unifier u = i.next();
                    Literal head = (Literal) n.getConsequence().capply(u);
                    if (head.getFunctor().equals(NormativeProgram.FailFunctor)) {
                        notifier.failure(head);
                        throw new NormativeFailureException((Structure) head);
                    }
                }
            }

            // -- computes new obligations, permissions, and prohibitions
            for (INorm n : regulativeNorms.values()) {
                Iterator<Unifier> i = n.getCondition().logicalConsequence(ag, new Unifier());
                while (i.hasNext()) {
                    var ni = new NormInstance((Literal) n.getConsequence(), i.next(), n);
                    if (checkNewNormInstance(ni))
                        newObl.add(ni);
                }
            }
        }
        oblTransitions.update();
        return newObl;
    }

    private boolean checkNewNormInstance(NormInstance ni) {
        // check if already in BB
        if (!containsIgnoreDeadline(getActive(), ni)) { // is it a new obligation?
            if (ni.isMaintenanceCondFromNorm || holds(ni.getMaintenanceCondition())) { // is the maintenance condition true?, avoids the creation of unnecessary obligations
                if ((ni.isObligation() && !holds(ni.getAim())) || // that is an obligation not achieved yet
                        (ni.isPermission() && !holds(ni.getAim())) || // or a permission not achieved yet
                        ni.isProhibition()                             // or a prohibition
                ) {
                    ni.setActive();
                    if (getAg().getBB().add(createState(ni))) {
                        addInSchedule(ni);
                        notifier.add(EventType.create, ni);
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private void addInSchedule(final NormInstance o) {
        long ttf = o.getTimeDeadline();
        if (ttf >= 0) { // the deadline is a moment/time
            ttf = ttf - System.currentTimeMillis();
            scheduler.schedule(() -> oblTransitions.deadlineAchieved(o), ttf, TimeUnit.MILLISECONDS);
        }
    }

    protected Literal createState(NormInstance o) {
        Literal s = ASSyntax.createLiteral(o.getState().name(), o);
        s.addSource(NormAtom);
        return s;
    }

    protected boolean containsIgnoreDeadline(Collection<NormInstance> list, NormInstance obl) {
        for (NormInstance l : list)
            if (l.equalsIgnoreDeadline(obl))
                return true;
        return false;
    }

    public String getNormsString() {
        StringBuilder out = new StringBuilder();
        for (INorm n : regimentedNorms.values())
            out.append(n + ".\n");
        for (INorm n : regulativeNorms.values())
            out.append(n + ".\n");
        for (var sr: sanctionRules.values()) {
            out.append(sr + ".\n");
        }
        return out.toString();
    }

    public String getStateString() {
        StringBuilder out = new StringBuilder("--- normative state ---\n\n");
        out.append("active:\n");
        for (var l : getActive()) {
            out.append("  " + l + "\n");
        }
        out.append("\nfulfilled:\n");
        for (var l : getFulfilled()) {
            out.append("  " + l + "\n");
        }
        out.append("\nunfulfilled:\n");
        for (var l : getUnFulfilled()) {
            out.append("  " + l + "\n");
        }
        out.append("\nsanctions:\n");
        for (var l : getCreatedSanctions()) {
            out.append("  " + l + "\n");
        }
        return out.toString();
    }

    public Element getAsDOM(Document document) {
        Element ele = (Element) document.createElement("normative-state");
        for (NormInstance l : getUnFulfilled())
            ele.appendChild(obligation2dom(document, l, State.unfulfilled, true));
        for (NormInstance l : getActive())
            ele.appendChild(obligation2dom(document, l, State.active, true));
        for (NormInstance l : getFulfilled())
            ele.appendChild(obligation2dom(document, l, State.fulfilled, false));
        for (NormInstance l : getInactive())
            ele.appendChild(obligation2dom(document, l, State.inactive, false));
        return ele;
    }

    private Element obligation2dom(Document document, NormInstance l, State state, boolean relativeTime) {
        Element oblele = (Element) document.createElement("deontic-modality");
        try {
            oblele.setAttribute("modality", l.getFunctor());
            oblele.setAttribute("state", state.name());
            oblele.setAttribute("agent", l.getAg().toString());
            if (l.isMaintenanceCondFromNorm && !"true".equals(l.getMaintenanceCondition().toString()))
                oblele.setAttribute("maintenance", "as in norm " + l.getNorm().getId());
            else
                oblele.setAttribute("maintenance", l.getMaintenanceCondition().toString());
            oblele.setAttribute("aim", l.getAim().toString());
            long ttf = l.getTimeDeadline();
            if (ttf >= 0) {
                if (relativeTime)
                    oblele.setAttribute("ttf", TimeTerm.toRelativeTimeStr(ttf));
                else
                    oblele.setAttribute("ttf", TimeTerm.toTimeStamp(ttf));
            } else {
                oblele.setAttribute("ttf", l.getStateDeadline().toString());
            }

            String toff = l.getDoneStr();
            if (toff != null) {
                oblele.setAttribute("done", toff); //TimeTerm.toAbsTimeStr(toff));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error adding attribute in DOM for " + l + " " + state, e);
        }

        try {
            if (l.hasAnnot()) {
                for (Term t : l.getAnnots()) {
                    if (t instanceof Literal) {
                        Literal la = (Literal) t;
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
            logger.log(Level.WARNING, "Error adding annotations in DOM for " + l + " " + state, e);
        }
        return oblele;
    }


    @Override
    public String toString() {
        return "normative interpreter";
    }

    /**
     * sets the update interval for checking the change in obligations' state
     */
    public void setUpdateInterval(int miliseconds) {
        oblTransitions.setUpdateInterval(miliseconds);
    }

    public enum EventType {create, fulfilled, unfulfilled, inactive}

    class Notifier extends Thread {
        ExecutorService exec = Executors.newFixedThreadPool(4); //SingleThreadExecutor(); //Executors.newCachedThreadPool();

        void add(EventType t, NormInstance o) {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    for (NormativeListener l : listeners)
                        try {
                            switch (t) {
                                case create:
                                    l.created(o.copy());
                                    break;
                                case fulfilled:
                                    l.fulfilled(o.copy());
                                    break;
                                case inactive:
                                    l.inactive(o.copy());
                                    break;
                                case unfulfilled:
                                    l.unfulfilled(o.copy());
                                    break;
                            }
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error notifying " + t + ":" + o + " to normative listener " + l, e);
                        }
                }
            });

            try {
                verifySanction(t, o);
            } catch (NPLInterpreterException e) {
                logger.warning(e.getMessage());
            }

        }

        void failure(Literal f) {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    for (NormativeListener l : listeners)
                        try {
                            l.failure((Structure) f.clone());
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error notifying " + f + " to normative listener " + l, e);
                        }
                }
            });
        }

        void sanction(String normId,EventType event, Literal f) {
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    for (NormativeListener l : listeners)
                        try {
                            l.sanction(normId, event, (Structure) f.clone());
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error notifying " + f + " to normative listener " + l, e);
                        }
                }
            });
        }
    }

    void verifySanction(EventType t, NormInstance o) throws NPLInterpreterException {
        switch (t) {
            case unfulfilled:
            case inactive:
            case fulfilled:
                for (Literal s: o.getNorm().ifUnfulfilledSanction()) {
                    var sApplied = s.capply(o.getUnifier());

                    // unifies with sanction args
                    var sRule = sanctionRules.get(s.getFunctor());
                    if (sRule == null) {
                        throw new NPLInterpreterException("Sanction id ("+s+") in norm "+o.getNorm().getId()+" was not found!");
                    }
                    var sLiteral = ASSyntax.createLiteral(sRule.getId());
                    for (var v: sRule.getArgs())
                        sLiteral.addTerm(v);
                    var un = new Unifier();
                    if (! un.unifies(sApplied,sLiteral)) {
                        throw new NPLInterpreterException("Sanction '"+sLiteral+"' does not unify with "+sApplied+" from norm "+o.getNorm().getId());
                    }

                    Iterator<Unifier> sols = null;
                    // verify sanction condition
                    if (sRule.getCondition() == null ||
                            ((sols = sRule.getCondition().logicalConsequence(getAg(), un)) != null) &&
                                    sols.hasNext()) {
                        if (sols != null)
                            un = sols.next();
                        //System.out.println("New unifier = "+un);

                        if (sRule.hasDeonticConsequence()) { // a sanction with an obligation as consequence
                            var ni = new NormInstance(sRule.getConsequence(), un, sRule);
                            ni.addAnnot(ASSyntax.createStructure("sanction", new Atom(sRule.getId()), NormInstance.getUnifierAsTerm(un)));
                            if (checkNewNormInstance(ni)) {
                                notifier.sanction(o.getNorm().getId(), t, ni);
                                //System.out.println("New o-sanction = "+ni);
                            }

                        } else {
                            Literal newSaction = (Literal) sRule.getConsequence().capply(un);
                            //System.out.println("New r-sanction = "+newSaction);
                            newSaction.addAnnot(ASSyntax.createStructure("created", new TimeTerm(0, null)));
                            newSaction.addAnnot(ASSyntax.createStructure("norm", new Atom(o.getNorm().getId()), new Atom(t.name()), sApplied));
                            newSaction.addAnnot(ASSyntax.createStructure("sanction", new Atom(sRule.getId()), NormInstance.getUnifierAsTerm(un)));
                            notifier.sanction(o.getNorm().getId(), t, newSaction);

                            Literal newSactionBB = ASSyntax.createLiteral(CSANCTION.getFunctor(), newSaction).addSource(NormAtom);
                            getAg().getBB().add(newSactionBB);
                        }
                    }
                }
                break;
        }
    }
}
