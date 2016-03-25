package npl;

import jason.asSyntax.Structure;

public interface NormativeListener {
    public void created(Structure o);
    public void fulfilled(Structure o);
    public void unfulfilled(Structure o);
    public void inactive(Structure o);
    public void failure(Structure f);
}
