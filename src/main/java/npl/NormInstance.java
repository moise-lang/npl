package npl;

import java.util.Date;
import java.util.List;

import jason.NoValueException;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;

/** The generic class for obligations, permissions, and prohibitions */
public class NormInstance extends LiteralImpl {

    public enum State {
        none, active, fulfilled, unfulfilled, inactive
    }

    INorm norm; // the norm that created this obligation
    Unifier un; // the unifier used in the activation of the norm
    State state = State.none;
    int agInstances = 0; // when the Ag is a var, this field count how many
                            // agent instances where created
    boolean isMaintenanceCondFromNorm = false;

    private static Unifier emptyUnif = new Unifier();

    public NormInstance(Literal l, Unifier un, INorm n) {
        super(l.getFunctor());
        isMaintenanceCondFromNorm = l.getTerm(1).equals(n.getCondition());

        Literal lc = (Literal) l.capply(un);
        addTerm(lc.getTerm(0));
        addTerm(lc.getTerm(1));
        addTerm(lc.getTerm(2));
        addTerm(lc.getTerm(3));
        if (lc.hasAnnot())
            setAnnots(lc.getAnnots());
        this.norm = n;
        if (un == null)
            this.un = emptyUnif;
        else
            this.un = un.clone();
    }

    // used by capply
    protected NormInstance(NormInstance l, Unifier un) {
        super(l.getFunctor());
        for (Term t : l.getTerms())
            addTerm(t.capply(un));
        if (l.hasAnnot())
            setAnnots((ListTerm) l.getAnnots().capply(un));
        this.norm = l.norm;
        this.state = l.state;
        this.un = un;
        un.compose(l.un);
        this.isMaintenanceCondFromNorm = l.isMaintenanceCondFromNorm;
    }

    // used by copy
    private NormInstance(NormInstance d) {
        super(d);
        this.norm = d.norm;
        this.state = d.state;
        this.un = d.un;
        this.isMaintenanceCondFromNorm = d.isMaintenanceCondFromNorm;
    }

    String getActivatedNormUniqueId() {
        return norm.getId() + un.toString();
    }

    /** returns the norms used to create this obligation */
    public INorm getNorm() {
        return norm;
    }

    /**
     * returns the unifier used to activate the norm used to create this
     * obligation
     */
    public Unifier getUnifier() {
        return un;
    }

    public Literal getAg() {
        return (Literal) getTerm(0);
    }

    public LogicalFormula getMaintenanceCondition() {
        return (Literal) getTerm(1);
    }

    public LogicalFormula getAim() {
        return (LogicalFormula) getTerm(2);
    }

    /** gets the deadline (as a precise moment) */
    public long getTimeDeadline() {
        try {
            var time = (long) ((NumberTerm) getTerm(3)).solve();
            var now  = new Date().getTime();
            if (time < (now - (12*24*60*1000))) {
                // the deadline is too on the past, no `now` + in the deadline expression: add `now`
                time = time + new Date().getTime();
            }
            return time;
        } catch (NoValueException|ClassCastException e) {
            return -1;
        }
    }
    public LogicalFormula getStateDeadline() {
        try {
            return (LogicalFormula)getTerm(3);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public String getDoneStr() {
        long toff = -1;
        List<Term> al = getAnnots("done");
        if (!al.isEmpty()) {
            Structure annot = (Structure) al.get(0);
            try {
                toff = (long) ((NumberTerm) annot.getTerm(0)).solve();
                return TimeTerm.toTimeSliceStr(toff);
            } catch (NoValueException e) {
            }
        }
        return null;
    }

    public State getState() {
        return state;
    }

    public void incAgInstance() {
        agInstances++;
    }

    public int getAgIntances() {
        return agInstances;
    }

    public void setActive() {
        state = State.active;
        addAnnot(ASSyntax.createStructure("created", new TimeTerm(0, null)));
        addAnnot(ASSyntax.createStructure("norm", new Atom(norm.getId()), getUnifierAsTerm(un)));
        // compute the timestamp of deadline
        long ttf = getTimeDeadline();
        if (ttf >= 0) { // the deadline is a moment/time
            setTerm(3, new TimeTerm(new Date(ttf)));
            resetHashCodeCache();
        }
    }

    public ListTerm getUnifierAsTerm() {
        return getUnifierAsTerm(this.un);
    }

    public static ListTerm getUnifierAsTerm(Unifier un) {
        ListTerm lf = new ListTermImpl();
        ListTerm tail = lf;
        for (VarTerm k : un) {
            if (!k.isUnnamedVar()) {
                Term vl = un.get(k);
                tail = tail.append(ASSyntax.createList(ASSyntax.createString(k), vl));
            }
        }
        return lf;
    }

    public void setFulfilled() {
        state = State.fulfilled;
        long ttf = System.currentTimeMillis() - getTimeDeadline();
        addAnnot(ASSyntax.createStructure("done", ASSyntax.createNumber(ttf)));
        addAnnot(ASSyntax.createStructure("fulfilled", new TimeTerm(0, null)));
    }

    public void setUnfulfilled() {
        state = State.unfulfilled;
        addAnnot(ASSyntax.createStructure("unfulfilled", new TimeTerm(0, null)));
    }

    public void setInactive() {
        state = State.inactive;
        addAnnot(ASSyntax.createStructure("inactive", new TimeTerm(0, null)));
    }

    public boolean equalsIgnoreDeadline(NormInstance o) {
        return getFunctor().equals(o.getFunctor()) && getAg().equals(o.getAg()) &&
        // getReason().equals(o.getReason()) &&
                getAim().equals(o.getAim());
    }

    public NormInstance capply(Unifier u) {
        return new NormInstance(this, u);
    }

    public NormInstance copy() {
        return new NormInstance(this);
    }

    @Override
    public Term clone() {
        return copy();
    }

    public boolean isObligation() {
        return getFunctor().equals(NormativeProgram.OblFunctor);
    }

    public boolean isPermission() {
        return getFunctor().equals(NormativeProgram.PerFunctor);
    }

    public boolean isProhibition() {
        return getFunctor().equals(NormativeProgram.ProFunctor);
    }

    @Override
    public String toString() {
        StringBuilder so = new StringBuilder();
        so.append(getFunctor() + "(" + getAg() + ",");
        so.append(getMaintenanceCondition());
        so.append("," + getAim()+",");
        so.append(getTerm(3));
        so.append(")");
        if (hasAnnot()) {
            so.append(getAnnots());
        }
        return so.toString();
    }
}
