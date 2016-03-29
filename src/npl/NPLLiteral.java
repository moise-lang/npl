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
                if (l.getFunctor().equals("obligation") && !(l.getTerm(0).isAtom() || l.getTerm(0).isVar()))
                    throw new ParseException("First argument of obligations must be an agent or a variable.");
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
