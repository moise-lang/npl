package npl;

import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.PredicateIndicator;

import java.util.Collection;
import java.util.Iterator;

public interface DynamicFactsProvider {
    public boolean           isRelevant(PredicateIndicator pi);
    public Iterator<Unifier> consult(Literal l, Unifier u);
    
    public default Iterator<Unifier> consultFromCollection(final Literal l, final Unifier u, final Collection<? extends Literal> facts) {
        return new Iterator<Unifier>() {
            Iterator<? extends Literal> i  = facts.iterator();
            Unifier current      = null;
            boolean needsUpdate  = true;

            public boolean hasNext() {
                if (needsUpdate)
                    get();
                return current != null;
            }
            public Unifier next() {
                if (needsUpdate)
                    get();
                Unifier a = current;
                if (current != null)
                    needsUpdate = true;
                return a;
            }
            private void get() {
                needsUpdate = false;
                current     = null;
                while (i.hasNext()) {
                    Unifier uc = u.clone();
                    if (uc.unifiesNoUndo(l, i.next())) {
                        current = uc;
                        return;
                    }
                }
            }
            public void remove() {}
        };
    }

    public default Iterator<Unifier> consultFromProviders(final Literal l, final Unifier u, final Iterator<? extends DynamicFactsProvider> providers) {
        return new Iterator<Unifier>() {
            Iterator<Unifier> i  = null;
            Unifier current      = null;
            boolean needsUpdate  = true;

            public boolean hasNext() {
                if (needsUpdate)
                    get();
                return current != null;
            }
            public Unifier next() {
                if (needsUpdate)
                    get();
                Unifier a = current;
                if (current != null)
                    needsUpdate = true;
                return a;
            }
            private void get() {
                needsUpdate = false;
                current     = null;
                if (i == null)
                    if (providers.hasNext()) {
                        DynamicFactsProvider d = providers.next();
                        if (d.isRelevant(l.getPredicateIndicator())) {
                            i = d.consult(l, u);
                        } else {
                            get();
                            return;
                        }
                    } else {
                        return;
                    }
                if (i.hasNext()) {
                    current = i.next();
                } else {
                    i = null;
                    get();
                }
            }
            public void remove() {}
        };
    }


}
