package npl;

import jason.asSemantics.Agent;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.Term;

import java.util.Iterator;

import npl.parser.ParseException;

public class NPLLiteral extends LiteralImpl {

    // used by the parser
    public static LiteralFactory getFactory() {
        return new LiteralFactory() {
            public Literal createNPLLiteral(Literal l, DynamicFactsProvider dfp) throws ParseException {
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
        if (dfp != null && dfp.isRelevant(getPredicateIndicator()))
            return dfp.consult(this, un);
        else
            return super.logicalConsequence(ag, un);
    }
 
    @Override
    public Term capply(Unifier u) {
        return new NPLLiteral(this, dfp, u);
    }
    
    public Term clone() {
        return new NPLLiteral(this, dfp);
    }

}
