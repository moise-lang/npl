package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.VarTerm;

import java.util.List;

public class SanctionRule extends AbstractSanctionRule {

    public SanctionRule(String id, List<VarTerm> args, LogicalFormula condition, Literal consequence) {
        this.id = id;
        this.consequence = consequence;
        this.condition = condition;
        this.args.addAll(args);
    }

    @Override
    public SanctionRule cloneSanction() {
        return new SanctionRule(id, this.args, (LogicalFormula) condition.clone(), consequence.copy());
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
        return "sanction " + getId() + sArgs + sCond + " -> " + getConsequence();
    }
}
