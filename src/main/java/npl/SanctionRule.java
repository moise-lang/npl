package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import npl.parser.ParseException;

public class SanctionRule extends AbstractSanctionRule {

    public SanctionRule(Literal trigger, LogicalFormula condition, Literal consequence) throws ParseException {
        if (!consequence.getFunctor().equals("sanction"))
            throw new ParseException("sanction-rule consequence must use functor 'sanction' instead of "+trigger);
        if (consequence.getArity() != 2)
            throw new ParseException("sanction-rule consequence fact should have two terms: the agent and the sanction");
        this.trigger = trigger;
        this.consequence = consequence;
        this.condition = condition;
    }

    @Override
    public SanctionRule cloneSanction() {
        try {
            return new SanctionRule(trigger.copy(), (condition==null ? null : (LogicalFormula) condition.clone()), consequence.copy());
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        var sCond = new StringBuilder();
        if (getCondition() != null) {
            sCond.append(": ");
            sCond.append(getCondition());
        }
        return "sanction-rule " + trigger.toString() + sCond + " -> " + getConsequence();
    }
}
