package npl;

import jason.NoValueException;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

/** The generic class for obligations, permissions, and prohibitions */
public class DeonticModality extends LiteralImpl {

    public enum State { none, active, fulfilled, unfulfilled, inactive } ;

    Norm n; // the norm that created this obligation
    State s = State.none;
    
    public DeonticModality(Literal l, Unifier u, Norm n) {
        super(l.getFunctor());
        Literal lc = (Literal)l.capply(u);
        Unifier newu = new Unifier();
        newu.unifies(l.getTerm(0), lc.getTerm(0)); // unifies agent
        newu.unifies(l.getTerm(2), lc.getTerm(2)); // unifies aim, this unifier is used for the maint. cond.
        addTerm(lc.getTerm(0));
        addTerm(l.getTerm(1).capply(newu));
        addTerm(lc.getTerm(2));
        addTerm(lc.getTerm(3));
        if (lc.hasAnnot())
            setAnnots( lc.getAnnots() );

        this.n = n;
    }
    
    // used by capply
    private DeonticModality(DeonticModality l, Unifier u) {
        super(l.getFunctor());
        Literal lc = (Literal)l.capply(u);
        Unifier newu = new Unifier();
        newu.unifies(l.getTerm(0), lc.getTerm(0)); // unifies agent
        newu.unifies(l.getTerm(2), lc.getTerm(2)); // unifies aim, this unifier is used for the maint. cond.
        addTerm(lc.getTerm(0));
        addTerm(l.getTerm(1).capply(newu));
        addTerm(lc.getTerm(2));
        addTerm(lc.getTerm(3));
        if (lc.hasAnnot())
            setAnnots( lc.getAnnots() );

        this.n = l.n;
        this.s = l.s;
    }
    
    // used by copy
    private DeonticModality(DeonticModality d) {
        super(d);
        this.n = d.n;
        this.s = d.s;
    }
    
    /** returns the norms used to create this obligation */
    public Norm getNorm() {
        return n;
    }
    
    public Literal getAg() {
        return (Literal)getTerm(0);
    }

    public LogicalFormula getMaitenanceCondition() {
        return (Literal)getTerm(1);
    }
    
    public LogicalFormula getAim() {
        return (LogicalFormula)getTerm(2);
    }
    
    /** gets the deadline (in milliseconds) */
    public long getDeadline() {
        try {
            return (long)((NumberTerm)getTerm(3)).solve();
        } catch (NoValueException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public State getState() {
        return s;
    }
    
    public void setActive() {
        s = State.active;
        addAnnot(ASSyntax.createStructure("created", new TimeTerm(0,null)));
    }
    
    public void setFulfilled() {
        s = State.fulfilled;
        long ttf = System.currentTimeMillis()-getDeadline();
        addAnnot(ASSyntax.createStructure("done", new TimeTerm(ttf, "milliseconds")));
        addAnnot(ASSyntax.createStructure("fulfilled", new TimeTerm(0,null)));
    }

    public void setUnfulfilled() {
        s = State.unfulfilled;
        addAnnot(ASSyntax.createStructure("unfulfilled", new TimeTerm(0,null)));
    }
    
    public void setInactive() {
        s = State.inactive;
        addAnnot(ASSyntax.createStructure("inactive", new TimeTerm(0,null)));
    }
    

    public boolean equalsIgnoreDeadline(DeonticModality o) {
        return getFunctor().equals(o.getFunctor()) &&
               getAg().equals(o.getAg()) && 
               //getReason().equals(o.getReason()) && 
               getAim().equals(o.getAim());
    }
    
    public DeonticModality capply(Unifier u) {
        return new DeonticModality(this, u);
    }
    public DeonticModality copy() {
        return new DeonticModality(this);
    }
    
    @Override
    public Term clone() {
        return copy();
    }
    
    public boolean isObligation()  { return getFunctor().equals(NormativeProgram.OblFunctor); } 
    public boolean isPermission()  { return getFunctor().equals(NormativeProgram.PerFunctor); } 
    public boolean isProhibition() { return getFunctor().equals(NormativeProgram.OblFunctor); } 

}

