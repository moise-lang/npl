package npl;

import jason.asSyntax.VarTerm;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSanctionRule extends AbstractNorm implements ISanctionRule {

    protected List<VarTerm> args = new ArrayList<>();

    @Override
    public List<VarTerm> getArgs() {
        return args;
    }

    @Override
    public boolean hasDeonticConsequence() { return false; }
}
