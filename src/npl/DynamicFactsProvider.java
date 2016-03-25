package npl;

import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.PredicateIndicator;

import java.util.Iterator;

public interface DynamicFactsProvider {
    public boolean           isRelevant(PredicateIndicator pi);
    public Iterator<Unifier> consult(Literal l, Unifier u);
}
