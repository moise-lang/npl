package npl;

import jason.asSyntax.Structure;

public interface NormativeListener {
    public void created(DeonticModality o);
    public void fulfilled(DeonticModality o);
    public void unfulfilled(DeonticModality o);
    public void inactive(DeonticModality o);
    public void failure(Structure f);
}
