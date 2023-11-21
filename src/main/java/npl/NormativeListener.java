package npl;

import jason.asSyntax.Structure;

public interface NormativeListener {
    public default void created(NormInstance o) {};
    public default void fulfilled(NormInstance o) {};
    public default void unfulfilled(NormInstance o) {};
    public default void inactive(NormInstance o) {};
    public default void failure(Structure f) {};
}
