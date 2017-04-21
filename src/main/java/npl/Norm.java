package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public class Norm {

	private String id;
	private Literal consequence;
	private LogicalFormula condition;

	public Norm(String id, Literal consequence, LogicalFormula condition) {
		if (!consequence.getFunctor().equals(NormativeProgram.FailFunctor)) {
			if (((Literal) consequence.getTerm(1)).getFunctor().toString().equals(id)) {
				consequence.setTerm(1, condition);
			}
		}
		this.id = id;
		this.consequence = consequence;
		this.condition = condition;
	}

	public String getId() {
		return id;
	}

	public Literal getConsequence() {
		return consequence;
	}

	public LogicalFormula getCondition() {
		return condition;
	}

	public Norm clone() {
		return new Norm(id, consequence.copy(), (LogicalFormula) condition.clone());
	}

	@Override
	public String toString() {
		return "norm " + id + ": " + condition + " -> " + consequence;
	}

}
