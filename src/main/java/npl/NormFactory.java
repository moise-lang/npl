package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public interface NormFactory {
    public INorm createNorm(String id, Literal consequence, LogicalFormula activationCondition);
    public INorm parseNorm(String norm, DynamicFactsProvider dfp) throws Exception;
}
