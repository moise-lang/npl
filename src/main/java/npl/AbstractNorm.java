package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

import java.util.List;

public abstract class AbstractNorm implements INorm {

    protected String id;
    protected LogicalFormula condition;
    protected Literal consequence;

    protected List<Literal> ifFulfilled;
    protected List<Literal> ifUnfulfilled;
    protected List<Literal> ifInactive;

//    @Override
//    public abstract AbstractNorm clone();

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

    @Override
    public INorm clone() { return null; }

    @Override
    public List<Literal> ifFulfilledSanction() { return ifFulfilled; }
    @Override
    public List<Literal> ifUnfulfilledSanction() { return ifUnfulfilled; }
    @Override
    public List<Literal> ifInactiveSanction() { return ifInactive; }

    @Override
    public void addFulfilledSanction(Literal sr) { ifFulfilledSanction().add(sr); }
    @Override
    public void addUnfulfilledSanction(Literal sr) { ifUnfulfilledSanction().add(sr); }
    @Override
    public void addInactiveSanction(Literal sr) { ifInactiveSanction().add(sr); }

    protected String sanctionsToStr(String intro, List<Literal> sList) {
        var out = new StringBuilder();
        if (!sList.isEmpty()) {
            out.append(intro);
            var v = "";
            for (Literal sr : sList) {
                out.append(v);
                out.append(sr);
                v = ", ";
            }
        }
        return out.toString();
    }
}
