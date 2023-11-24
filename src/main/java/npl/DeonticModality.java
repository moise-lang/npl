package npl;

import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;

/**
 * @deprecated renamed to NomeInstance
 */
@Deprecated
public class DeonticModality extends NormInstance {
    public DeonticModality(Literal l, Unifier u, INorm n) {
        super(l,u,n);
    }
}
