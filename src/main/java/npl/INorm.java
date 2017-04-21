package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

/**
 * @author igorcadelima
 *
 */
public interface INorm {

    public INorm clone();
    
    /**
     * Returns the id of the norm which was assigned at creation.
     * 
     * @return id
     */
    public String getId();

    /**
     * Returns formula that determines the activation condition for the norm.
     * 
     * @return activation condition
     */
    public LogicalFormula getCondition();

    /**
     * Returns consequence of the norm activation as a {@link Literal}. It could
     * be an obligation, permission, prohibition, or failure.
     * 
     * @return consequence
     * @see DeonticModality
     */
    public Literal getConsequence();
}
