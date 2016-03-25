package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public interface NormFactory {
    public Norm createNorm(String id, Literal head, LogicalFormula body);
}
