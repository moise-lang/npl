package npl;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.Term;

public class reset extends DefaultInternalAction {
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        var ag = (NormativeAg)ts.getAg();
        //ag.logger.info("** removing all facts from NPL **")
        ag.resetNPL();
        return true;
    }
}
