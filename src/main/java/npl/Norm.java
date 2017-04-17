package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.LogicalFormula;

public class Norm {

    // used by the parser
    public static NormFactory getFactory() {
        return new NormFactory() {
            public Norm createNorm(String id, Literal consequence, LogicalFormula activationCondition) {
                if (!consequence.getFunctor().equals(NormativeProgram.FailFunctor)) {
                    if ( ((Literal)consequence.getTerm(1)).getFunctor().toString().equals(id)) {
                        //consequence.addAnnot(ASSyntax.createStructure("norm", consequence.getTerm(1)));
                        consequence.setTerm(1, activationCondition);
                    //} else {
                        //consequence.addAnnot(ASSyntax.createStructure("norm", new Atom(id)));                        
                    }
                }
                return new Norm(id,consequence,activationCondition);
            }
        };
    }    

    private String id;
    private Literal consequence;
    private LogicalFormula condition;
    
    public Norm(String id, Literal head, LogicalFormula body) {
        this.id = id;
        this.consequence = head;
        this.condition = body;
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
        return new Norm(id, consequence.copy(), (LogicalFormula)condition.clone());
    }
    
    @Override
    public String toString() {
        return "norm " + id + ": " + condition + " -> " + consequence;
    }
    
}
