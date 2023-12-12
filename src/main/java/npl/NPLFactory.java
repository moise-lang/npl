package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import npl.parser.ParseException;
import npl.parser.nplp;

import java.io.StringReader;

public class NPLFactory implements INPLFactory {
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
    public ISanctionRule createSanctionRule(Literal trigger, LogicalFormula condition, Literal consequence) throws ParseException {
        return new SanctionRule(trigger, condition, consequence);
    }
}
