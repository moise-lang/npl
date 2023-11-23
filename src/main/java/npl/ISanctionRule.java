package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.VarTerm;

import java.util.List;

/**
 * @author Jomi
 */
public interface ISanctionRule extends INorm {

    ISanctionRule cloneSanction();


    List<VarTerm> getArgs();

    boolean hasDeonticConsequence();

}
