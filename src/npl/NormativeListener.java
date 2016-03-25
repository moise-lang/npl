package npl;

import jason.asSyntax.Structure;

public interface NormativeListener {
    public void created(Obligation o);
    public void fulfilled(Obligation o);
    public void unfulfilled(Obligation o);
    public void inactive(Obligation o);
    public void failure(Structure f);
}
