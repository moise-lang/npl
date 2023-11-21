package npl;

import java.io.StringReader;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import npl.parser.nplp;

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
    }

    @Override
    public Norm clone() {
        return new Norm(id, consequence.copy(), (LogicalFormula) condition.clone());
    }

    @Override
    public String toString() {
        return "norm " + id + ": " + condition + " -> " + consequence;
    }
}
