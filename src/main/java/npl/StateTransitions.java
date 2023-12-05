package npl;

import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * this class updates the state of obligations, permissions and prohibitions (e.g. active -> fulfilled)
 */
public class StateTransitions {

    protected Queue<NormInstance> toCheckUnfulfilledByDeadline = new ConcurrentLinkedQueue<>();

    protected NPLInterpreter engine;

    public StateTransitions(NPLInterpreter engine) {
        this.engine = engine;
    }

    public void start() {}
    public void stop() {}
    public void setUpdateInterval(int miliseconds) {
        engine.logger.warning("This State Transition implementation does not support time interval updates!");
    }

    /**
     * update the state of the obligations
     */
    void update() {
        updateActive();
        updateInactive();
        updateDeadline();
        updateDoneForUnfulfilled();
    }

    synchronized void deadlineAchieved(NormInstance o) {
        toCheckUnfulfilledByDeadline.offer(o);
    }

    // -- transition active -> (un)fulfilled (based on aim)
    private void updateActive() {
        var active = engine.getActive();
        var bb = engine.getAg().getBB();
        for (NormInstance o : active) {
            var oasinbb = engine.createState(o);
            if (o.isObligation()) {
                // transition active -> fulfilled
                if (o.getAg().isGround() && engine.holds(o.getAim())) {
                    if (!bb.remove(oasinbb)) engine.logger.log(Level.FINE, "ooops " + oasinbb + " should be removed 2");
                    o = o.copy();
                    o.setFulfilled();
                    bb.add(engine.createState(o));
                    engine.notifier.add(NPLInterpreter.EventType.fulfilled, o);
                } else {
                    List<NormInstance> fuls = engine.getFulfilledObligations();
                    Iterator<Unifier> i = o.getAim().logicalConsequence(engine.getAg(), new Unifier());
                    while (i.hasNext()) {
                        Unifier u = i.next();
                        NormInstance obl = new NormInstance(new LiteralImpl(o), u, o.getNorm());
                        if (!engine.containsIgnoreDeadline(fuls, obl)) {
                            o.incAgInstance();
                            obl.setFulfilled();
                            bb.add(engine.createState(obl));
                            engine.notifier.add(NPLInterpreter.EventType.fulfilled, obl);
                        }
                    }
                }
            } else if (o.isProhibition()) {
                // transition active -> unfulfilled
                if (o.getAg().isGround() && engine.holds(o.getAim())) { // the case of a prohibition for one agent
                    if (!bb.remove(oasinbb)) engine.logger.log(Level.FINE, "ooops " + oasinbb + " should be removed 2");
                    o = o.copy();
                    o.setUnfulfilled();
                    bb.add(engine.createState(o));
                    engine.notifier.add(NPLInterpreter.EventType.unfulfilled, o);
                } else {
                    List<NormInstance> unfuls = engine.getUnFulfilledProhibitions();
                    Iterator<Unifier> i = o.getAim().logicalConsequence(engine.getAg(), new Unifier());
                    while (i.hasNext()) {
                        Unifier u = i.next();
                        NormInstance obl = new NormInstance(new LiteralImpl(o), u, o.getNorm());
                        if (!engine.containsIgnoreDeadline(unfuls, obl)) {
                            o.incAgInstance();
                            obl.setUnfulfilled();
                            bb.add(engine.createState(obl));
                            engine.notifier.add(NPLInterpreter.EventType.unfulfilled, obl);
                        }
                    }
                }
            }
        }
    }

    // -- transition active -> inactive (based on while for obl/pro/per)
    //                                  (based also on what for per)
    private void updateInactive() {
        var active = engine.getActive();
        var bb = engine.getAg().getBB();
        for (NormInstance o : active) {
            if (!engine.holds(o.getMaintenanceCondition()) || (o.isPermission() && engine.holds(o.getAim()))) {
                Literal oasinbb = engine.createState(o);
                if (!bb.remove(oasinbb))
                    engine.logger.log(Level.INFO, "ooops " + oasinbb + " should be removed 1!");
                o.setInactive();
                engine.notifier.add(NPLInterpreter.EventType.inactive, o);
            }
        }
    }

    // -- transition active -> unfulfilled (for obl) (based on deadline)
    //               active -> inactive (for per)
    //               active -> fulfilled (for pro)
    private void updateDeadline() {
        var active = engine.getActive();
        var o = toCheckUnfulfilledByDeadline.poll(); // norm instances that have a time based deadline, if they are still active (i.e., not fulfilled), they should be moved to unfulfilled
        while (o != null) {
            if (engine.containsIgnoreDeadline(active, o)) { // deadline achieved, and still active
                niAchievedDeadline(o);
            }
            o = toCheckUnfulfilledByDeadline.poll();
        }

        // test deadline that are logical expressions
        for (var ni: active) {
            if (ni.getStateDeadline() != null) {
                if (engine.holds(ni.getStateDeadline())) {
                    niAchievedDeadline(ni);
                }
            }
            // TODO: check also deadline of NI (4th argument), in case there is no schedule
        }
    }

    private void niAchievedDeadline(NormInstance o) {
        var oasinbb = engine.createState(o);
        var bb = engine.getAg().getBB();

        if (!bb.remove(oasinbb))
            engine.logger.log(Level.INFO, "ooops 3 " + o + " should be removed (due the deadline), but it is not in the set of facts.");

        if (o.isObligation()) {
            // transition for obligation (active -> unfulfilled)
            if (o.getAgIntances() == 0) { // it is unfulfilled only if no agent instance has fulfilled the prohibition
                o.setUnfulfilled();
                bb.add(engine.createState(o));
                engine.notifier.add(NPLInterpreter.EventType.unfulfilled, o);
            }
        } else if (o.isPermission()) {
            // transition for permission (active -> inactive)
            o.setInactive();
            bb.add(engine.createState(o));
            engine.notifier.add(NPLInterpreter.EventType.inactive, o);
        } else {
            // transition for prohibition (active -> fulfilled)
            if (o.getAgIntances() == 0) { // it is fulfilled only if no agent instance has unfulfilled the prohibition
                o.setFulfilled();
                bb.add(engine.createState(o));
                engine.notifier.add(NPLInterpreter.EventType.fulfilled, o);
            }
        }
        try {
            engine.verifyNorms();
        } catch (NormativeFailureException e) {
            //System.err.println("Error to set obligation "+o+" to unfulfilled!");
            //e.printStackTrace();
        }
    }

    private void updateDoneForUnfulfilled() {
        // check done for unfulfilled and inactive
        List<NormInstance> unfulObl = engine.getUnFulfilledObligations();
        List<NormInstance> unfulPlusInactObls = engine.getInactiveObligations();
        unfulPlusInactObls.addAll(unfulObl);
        for (NormInstance o : unfulPlusInactObls) {
            if (engine.holds(o.getAim()) && o.getAnnots("done").isEmpty()) { // if the agent did, even latter...
                long ttf = System.currentTimeMillis() - o.getTimeDeadline();
                o.addAnnot(ASSyntax.createStructure("done", new TimeTerm(ttf, "milliseconds")));
            }
        }
    }

}
