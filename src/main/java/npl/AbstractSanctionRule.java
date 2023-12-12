package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public abstract class AbstractSanctionRule implements ISanctionRule {

    protected Literal trigger;
    protected LogicalFormula condition;
    protected Literal consequence;

    @Override
    public Literal getTrigger() {
        return trigger;
    }

    @Override
    public LogicalFormula getCondition() {
        return condition;
    }

    @Override
    public Literal getConsequence() {
        return consequence;
    }
}
