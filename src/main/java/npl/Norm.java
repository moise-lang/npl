package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public class Norm extends AbstractNorm {

    /**
     * Creates a norm based on the given arguments. If consequence is not a
     * failure and maintenance condition's functor is equal to the norm's id,
     * then consequence's maintenance condition becomes the condition passed as
     * argument.
     * 
     * @param id
     *            norm's id
     * @param consequence
     *            norm's consequence
     * @param condition
     *            norm's activation condition
     */
    public static NormFactory getFactory() {
        return new NormFactory() {
            public Norm createNorm(String id, Literal consequence, LogicalFormula activationCondition) {
                boolean consequenceIsFailure = consequence.getFunctor().equals(NormativeProgram.FailFunctor);
                if (!consequenceIsFailure) {
                    String maintenanceConditionFunctor = ((Literal) consequence.getTerm(1)).getFunctor();
                    if (maintenanceConditionFunctor.equals(id)) {
                        consequence.setTerm(1, activationCondition);
                    }
                }
                return new Norm(id, consequence, activationCondition);
            }
        };
    }

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
