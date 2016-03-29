package npl;

import npl.parser.ParseException;
import jason.asSyntax.Literal;


public interface LiteralFactory {
    public Literal createNPLLiteral(Literal l, DynamicFactsProvider dfp) throws ParseException;
}
