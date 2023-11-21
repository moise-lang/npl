package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.VarTerm;
import npl.parser.nplp;

import java.io.StringReader;
import java.util.List;

public class FactoryNPL implements IFactoryNPL {
    public INorm createNorm(String id, Literal consequence, LogicalFormula activationCondition) {
        boolean consequenceIsFailure = consequence.getFunctor().equals(NormativeProgram.FailFunctor);
        if (!consequenceIsFailure) {
            String maintenanceConditionFunctor = ((Literal) consequence.getTerm(1)).getFunctor();
            if (maintenanceConditionFunctor.equals(id)) {
                consequence.setTerm(1, activationCondition);
            }
        }
        return new Norm(id, consequence, activationCondition);
    }
    public INorm parseNorm(String norm, DynamicFactsProvider dfp) throws Exception {
        nplp parser = new nplp(new StringReader(norm));
        parser.setDFP(dfp);
        return parser.norm();
    }

    @Override
    public ISanctionRule createSanctionRule(String id, List<VarTerm> args, LogicalFormula activationCondition, Literal consequence) {
        return new SanctionRule(id, args, activationCondition, consequence);
    }
}
