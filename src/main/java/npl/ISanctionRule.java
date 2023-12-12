package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

/**
 * @author Jomi
 */
public interface ISanctionRule {

    Literal getTrigger();

    LogicalFormula getCondition();

    Literal getConsequence();

    ISanctionRule cloneSanction();
}
