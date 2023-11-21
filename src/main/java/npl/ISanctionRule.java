package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.VarTerm;

import java.util.List;

/**
 * @author Jomi
 */
public interface ISanctionRule {

    public ISanctionRule cloneSanction();

    /**
     * Returns the id of the sanction which was assigned at creation.
     *
     * @return id
     */
    public String getId();

    public List<VarTerm> getArgs();

    /**
     * Returns formula that determines the activation condition for the sanction.
     *
     * @return activation condition
     */
    public LogicalFormula getCondition();

    /**
     * Returns consequence of the sanction activation as a {@link Literal}.
     */
    public Literal getConsequence();
}
