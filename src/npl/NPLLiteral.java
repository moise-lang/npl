package npl;

import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Term;

import java.util.Iterator;

public class NPLLiteral extends LiteralImpl {

    // used by the parser
    public static LiteralFactory getFactory() {
        return new LiteralFactory() {
            public Literal createNPLLiteral(Literal l, DynamicFactsProvider dfp) {
                return new NPLLiteral(l,dfp);
            }
        };
    }    
    
    private DynamicFactsProvider dfp = null;    

    public NPLLiteral(Literal l, DynamicFactsProvider dfp) {
        super(l);
        this.dfp = dfp;
    }
        
    // used by capply
    protected NPLLiteral(Literal l, DynamicFactsProvider dfp, Unifier u) {
        super(l,u);
        this.dfp = dfp;
    }
    @Override
    public Iterator<Unifier> logicalConsequence(Agent ag, Unifier un) {
        return dfp.consult(this, un);
    }
 
    @Override
    public Term capply(Unifier u) {
        return new NPLLiteral(this, dfp, u);
    }
    
    public Term clone() {
        return new NPLLiteral(this, dfp);
    }

}
