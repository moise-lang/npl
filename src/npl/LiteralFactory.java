package npl;

import jason.asSyntax.Literal;

public interface LiteralFactory {
    public Literal createNPLLiteral(Literal l, DynamicFactsProvider dfp);
}
