package npl;

import jason.JasonException;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;

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
        try {
            updateActive();
            updateInactive();
            updateDeadline();
            updateDoneForUnfulfilled();
        } catch (JasonException e) {
            e.printStackTrace();
        }
    }

    synchronized void deadlineAchieved(NormInstance o) {
        toCheckUnfulfilledByDeadline.offer(o);
    }

    // -- transition active -> (un)fulfilled (based on aim)
    private void updateActive() throws JasonException {
        var active = engine.getActive();
        var bb = engine.getAg().getBB();
        for (NormInstance ni : active) {
            var oasinbb = engine.createState(ni);
            if (ni.isObligation()) {
                // transition active -> fulfilled
                if (ni.getAg().isGround() && engine.holds(ni.getAim())) {
                    if (!bb.remove(oasinbb)) engine.logger.log(Level.FINE, "ooops " + oasinbb + " should be removed 2");
                    ni = ni.copy();
                    ni.setFulfilled();
                    bb.add(engine.createState(ni));
                    engine.notifier.add(NPLInterpreter.EventType.fulfilled, ni);
                } else {
                    List<NormInstance> fuls = engine.getFulfilledObligations();
                    Iterator<Unifier> i = ni.getAim().logicalConsequence(engine.getAg(), ni.getUnifier().clone());
                    while (i.hasNext()) {
                        var un = i.next();
                        var obl = new NormInstance(ni, un);
                        if (!engine.containsIgnoreDeadline(fuls, obl)) {
                            ni.incAgInstance();
                            obl.setFulfilled();
                            bb.add(engine.createState(obl));
                            engine.notifier.add(NPLInterpreter.EventType.fulfilled, obl);
                        }
                    }
                }
            } else if (ni.isProhibition()) {
                // transition active -> unfulfilled
                if (ni.getAg().isGround() && engine.holds(ni.getAim())) { // the case of a prohibition for one agent
                    try {
                        if (!bb.remove(oasinbb))
                            engine.logger.log(Level.FINE, "ooops " + oasinbb + " should be removed 2");
                    } catch (Exception e) {
                        engine.logger.log(Level.FINE, "ooops " + oasinbb + " should be removed 2 "+e.getMessage());
                    }
                    ni = ni.copy();
                    ni.setUnfulfilled();
                    bb.add(engine.createState(ni));
                    engine.notifier.add(NPLInterpreter.EventType.unfulfilled, ni);
                } else {
                    var unfuls = engine.getUnFulfilledProhibitions();
                    var i = ni.getAim().logicalConsequence(engine.getAg(), ni.getUnifier().clone());
                    while (i.hasNext()) {
                        var un = i.next();
                        var pro = new NormInstance(ni, un);
                        if (!engine.containsIgnoreDeadline(unfuls, pro)) {
                            ni.incAgInstance();
                            pro.setUnfulfilled();
                            bb.add(engine.createState(pro));
                            engine.notifier.add(NPLInterpreter.EventType.unfulfilled, pro);
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
    private void updateDeadline() throws JasonException {
        var active = engine.getActive();
        var updateAct = false;
        var o = toCheckUnfulfilledByDeadline.poll(); // norm instances that have a time based deadline, if they are still active (i.e., not fulfilled), they should be moved to unfulfilled
        while (o != null) {
            if (engine.containsIgnoreDeadline(active, o)) { // deadline achieved, and still active
                niAchievedDeadline(o);
                updateAct = true;
            }
            o = toCheckUnfulfilledByDeadline.poll();
        }

        if (updateAct)
            active = engine.getActive(); // update active

        // test deadline that are logical expressions
        for (var ni: active) {
            if (ni.getStateDeadline() != null) {
                if (engine.holds(ni.getStateDeadline())) {
                    // TODO: create new instance with agent arg ground, if possible
                    //engine.logger.info("unfulfilled by state deadline "+ni);
                    niAchievedDeadline(ni);
                }
            } else {
                // check also deadline of NI (4th argument), in case there is no scheduler and the argument is time based
                if (ni.getTimeDeadline() >= 0) {
                    //engine.logger.info("check deadline "+ni.getTimeDeadline()+" now is "+System.currentTimeMillis());
                    if (System.currentTimeMillis() > ni.getTimeDeadline()) {
                        niAchievedDeadline(ni);
                    }
                }
            }
        }
    }

    private void niAchievedDeadline(NormInstance o) throws JasonException {
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
        var unfulObl = engine.getUnFulfilledObligations();
        var unfulPlusInactObls = engine.getInactiveObligations();
        unfulPlusInactObls.addAll(unfulObl);
        for (NormInstance o : unfulPlusInactObls) {
            if (engine.holds(o.getAim()) && o.getAnnots("done").isEmpty()) { // if the agent did, even latter...
                long ttf = System.currentTimeMillis() - o.getTimeDeadline();
                o.addAnnot(ASSyntax.createStructure("done", new TimeTerm(ttf, "milliseconds")));
                //engine.logger.info("**** done too late "+o);
            }
        }
    }

    public void addInSchedule(final NormInstance o) {
        // do nothing, should be overridden by subclasses
    }
}
