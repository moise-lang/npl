package npl;

import jason.asSyntax.Structure;

public interface NormativeListener {
    public default void created(DeonticModality o) {};
    public default void fulfilled(DeonticModality o) {};
    public default void unfulfilled(DeonticModality o) {};
    public default void inactive(DeonticModality o) {};
    public default void failure(Structure f) {};
}
