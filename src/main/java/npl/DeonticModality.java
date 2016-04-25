package npl;

import jason.NoValueException;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;

import java.util.List;

/** The generic class for obligations, permissions, and prohibitions */
public class DeonticModality extends LiteralImpl {

    public enum State { none, active, fulfilled, unfulfilled, inactive } ;

    Norm n; // the norm that created this obligation
    State s = State.none;
    int   agInstances = 0; // when the Ag is a var, this field count how many agent instances where latter created 
    boolean maintContFromNorm = false;
    
    public DeonticModality(Literal l, Unifier u, Norm n) {
        super(l.getFunctor());
        maintContFromNorm = l.getTerm(1).equals(n.getCondition());
        
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
        this.maintContFromNorm = l.maintContFromNorm;
    }
    
    // used by copy
    private DeonticModality(DeonticModality d) {
        super(d);
        this.n = d.n;
        this.s = d.s;
        this.maintContFromNorm = d.maintContFromNorm;
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
    
    public String getDoneStr() {
        long toff = -1;
        List<Term> al = getAnnots("done");
        if (!al.isEmpty()) {
            Structure annot = (Structure)al.get(0);
            try {
                toff = (long)((NumberTerm)annot.getTerm(0)).solve();
                return TimeTerm.toAbsTimeStr(toff);
            } catch (NoValueException e) {  }
        }
        return null;
    }
    
    public State getState() {
        return s;
    }
    
    public void incAgInstance() {
        agInstances++;
    }
    public int getAgIntances() {
        return agInstances;
    }
    
    public void setActive() {
        s = State.active;
        addAnnot(ASSyntax.createStructure("created", new TimeTerm(0,null)));
    }
    
    public void setFulfilled() {
        s = State.fulfilled;
        long ttf = System.currentTimeMillis()-getDeadline();
        addAnnot(ASSyntax.createStructure("done", ASSyntax.createNumber(ttf)));
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
    public boolean isProhibition() { return getFunctor().equals(NormativeProgram.ProFunctor); } 

    
    @Override
    public String toString() {
        StringBuilder so = new StringBuilder();
        so.append(getFunctor()+"("+getAg()+",");
        //if (maintContFromNorm)
        //    so.append(getNorm().getId());
        //else
        so.append(getMaitenanceCondition());
        so.append(","+getAim()+",\"");
        if (s == State.active) {
            so.append(TimeTerm.toRelTimeStr( getDeadline()));
        } else { // if (s == State.fulfilled) {
            so.append(TimeTerm.toTimeStamp( getDeadline()));
        }
        so.append("\")");
        if (hasAnnot()) {
            so.append(getAnnots());
        }
        return so.toString();
    }
    
}

