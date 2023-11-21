package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.VarTerm;

import java.util.List;

public interface IFactoryNPL {
    public INorm createNorm(String id, Literal consequence, LogicalFormula activationCondition);
    public INorm parseNorm(String norm, DynamicFactsProvider dfp) throws Exception;

    default ISanctionRule createSanctionRule(String id, List<VarTerm> args, LogicalFormula activationCondition, Literal consequence) { return null; }

}
