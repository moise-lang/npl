package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public abstract class AbstractNorm implements INorm {

	protected String id;
	protected LogicalFormula condition;
	protected Literal consequence;

	@Override
	public abstract AbstractNorm clone();

	@Override
	public String getId() {
		return id;
	}

	@Override
	public LogicalFormula getCondition() {
		return condition;
	}

	@Override
	public Literal getConsequence() {
		return consequence;
	}
}