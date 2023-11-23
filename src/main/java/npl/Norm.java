package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

import java.util.ArrayList;
import java.util.List;

public class Norm extends AbstractNorm {

    /**
     * Creates a norm based on the arguments' value without any modification.
     *
     * @param id
     *            norm's id
     * @param consequence
     *            norm's consequence
     * @param condition
     *            norm's activation condition
     */
    public Norm(String id, Literal consequence, LogicalFormula condition) {
        this.id = id;
        this.consequence = consequence;
        this.condition = condition;
        ifFulfilled = new ArrayList<>();
        ifUnfulfilled = new ArrayList<>();
        ifInactive = new ArrayList<>();
    }

    @Override
    public Norm clone() {
        var n= new Norm(id, consequence.copy(), (LogicalFormula) condition.clone());
        n.ifFulfilled.addAll(this.ifFulfilled);
        n.ifUnfulfilled.addAll(this.ifUnfulfilled);
        n.ifInactive.addAll(this.ifInactive);
        return n;
    }

    @Override
    public String toString() {
        return "norm " + getId() + ": " + getCondition() + " -> " + getConsequence() +
                sanctionsToStr(" if fulfilled: ", ifFulfilledSanction()) +
                sanctionsToStr(" if unfulfilled: ", ifUnfulfilledSanction()) +
                sanctionsToStr(" if inactive: ", ifInactiveSanction());
    }

}
