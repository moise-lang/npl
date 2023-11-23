package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.VarTerm;

import java.util.ArrayList;
import java.util.List;

public class SanctionRule extends AbstractSanctionRule {

    public SanctionRule(String id, List<VarTerm> args, LogicalFormula condition, Literal consequence) {
        this.id = id;
        this.consequence = consequence;
        this.condition = condition;
        this.args.addAll(args);

        ifFulfilled = new ArrayList<>();
        ifUnfulfilled = new ArrayList<>();
        ifInactive = new ArrayList<>();
    }

    @Override
    public boolean hasDeonticConsequence() {
        return getConsequence().getFunctor().equals(NormativeProgram.OblFunctor) ||
                getConsequence().getFunctor().equals(NormativeProgram.PerFunctor) ||
                getConsequence().getFunctor().equals(NormativeProgram.ProFunctor);
    }

    @Override
    public SanctionRule cloneSanction() {
        var sr = new SanctionRule(id, this.args, (condition==null ? null : (LogicalFormula) condition.clone()), consequence.copy());
        sr.ifFulfilled.addAll(this.ifFulfilled);
        sr.ifUnfulfilled.addAll(this.ifUnfulfilled);
        sr.ifInactive.addAll(this.ifInactive);
        return sr;
    }

    @Override
    public String toString() {
        var sArgs = new StringBuilder();
        if (!args.isEmpty()) {
            sArgs.append("(");
            var v = "";
            for (VarTerm a : args) {
                sArgs.append(v);
                v = ",";
                sArgs.append(a);
            }
            sArgs.append(")");
        }
        var sCond = new StringBuilder();
        if (getCondition() != null) {
            sCond.append(": ");
            sCond.append(getCondition());
        }
        return "sanction " + getId() + sArgs + sCond + " -> " + getConsequence() +
                sanctionsToStr(" if fulfilled: ", ifFulfilledSanction()) +
                sanctionsToStr(" if unfulfilled: ", ifUnfulfilledSanction()) +
                sanctionsToStr(" if inactive: ", ifInactiveSanction());
    }
}
