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
	 *            Norm's id
	 * @param consequence
	 *            Norm's consequence
	 * @param condition
	 *            Norm's activation condition
	 */
	public Norm(String id, Literal consequence, LogicalFormula condition) {
		boolean consequenceIsFailure = consequence.getFunctor().equals(NormativeProgram.FailFunctor);
		if (!consequenceIsFailure) {
			String maintenanceConditionFunctor = ((Literal) consequence.getTerm(1)).getFunctor();
			if (maintenanceConditionFunctor.equals(id)) {
				consequence.setTerm(1, condition);
			}
		}
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