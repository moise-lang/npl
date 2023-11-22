package npl;

import jason.asSyntax.Structure;

public interface NormativeListener {
    default void created(NormInstance o) {}
    default void fulfilled(NormInstance o) {}
    default void unfulfilled(NormInstance o) {}
    default void inactive(NormInstance o) {}
    default void failure(Structure f) {}
    default void sanction(String normId, NPLInterpreter.EventType event, Structure sanction) {}

}
