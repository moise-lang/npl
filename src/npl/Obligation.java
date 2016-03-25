package npl;

import jason.NoValueException;
import jason.asSemantics.Unifier;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

public class Obligation extends LiteralImpl {

    Norm n; // the norm that created this obligation
    
    // used by capply
    public Obligation(Literal l, Unifier u, Norm n) {
        super("obligation");
        addTerm(l.getTerm(0).capply(u));
        addTerm(l.getTerm(1));
        addTerm(l.getTerm(2).capply(u));
        addTerm(l.getTerm(3).capply(u));
        if (l.hasAnnot())
            setAnnots( (ListTerm)l.getAnnots().capply(u) );

        this.n = n;
    }
    
    public Obligation(Literal l, Norm n) {
        super(l);
        this.n = n;
    }
    
    /** returns the norms used to create this obligation */
    public Norm getNorm() {
        return n;
    }
    
    public Term getAg() {
        return getTerm(0);
    }

    public LogicalFormula getMaitenanceCondition() {
        return (Literal)getTerm(1);
    }
    
    public Literal getAim() {
        return (Literal)getTerm(2);
    }
    
    public long getDeadline() {
        try {
            return (long)((NumberTerm)getTerm(3)).solve();
        } catch (NoValueException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean equalsIgnoreDeadline(Obligation o) {
        return getAg().equals(o.getAg()) && 
               //getReason().equals(o.getReason()) && 
               getAim().equals(o.getAim());
    }
    
    @Override
    public Obligation capply(Unifier u) {
        return new Obligation(this, u, this.n);
    }
    
    @Override
    public Obligation copy() {
        return new Obligation(this, n);
    }
    
    @Override
    public Term clone() {
        return copy();
    }

}

