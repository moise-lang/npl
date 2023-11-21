package npl;

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
    };

    INorm n; // the norm that created this obligation
    Unifier u; // the unifier used in the activation of the norm
    State s = State.none;
    int agInstances = 0; // when the Ag is a var, this field count how many
                            // agent instances where created
    boolean maintContFromNorm = false;

    private static Unifier emptyUnif = new Unifier();

    public NormInstance(Literal l, Unifier u, INorm n) {
        super(l.getFunctor());
        maintContFromNorm = l.getTerm(1).equals(n.getCondition());

        Literal lc = (Literal) l.capply(u);
        // Unifier newu = new Unifier();
        // newu.unifies(l.getTerm(0), lc.getTerm(0)); // unifies agent
        // newu.unifies(l.getTerm(2), lc.getTerm(2)); // unifies aim, this
        // unifier is used for the maint. cond.
        addTerm(lc.getTerm(0));
        // addTerm(l.getTerm(1).capply(newu));
        addTerm(lc.getTerm(1));
        addTerm(lc.getTerm(2));
        addTerm(lc.getTerm(3));
        if (lc.hasAnnot())
            setAnnots(lc.getAnnots());
        this.n = n;
        if (u == null)
            this.u = emptyUnif;
        else
            this.u = u.clone();
    }

    // used by capply
    private NormInstance(NormInstance l, Unifier u) {
        super(l.getFunctor());
        for (Term t : l.getTerms())
            addTerm(t.capply(u));
        if (l.hasAnnot())
            setAnnots((ListTerm) l.getAnnots().capply(u));
        /*
         * Literal lc = (Literal)l.capply(u); Unifier newu = new Unifier();
         * newu.unifies(l.getTerm(0), lc.getTerm(0)); // unifies agent
         * newu.unifies(l.getTerm(2), lc.getTerm(2)); // unifies aim, this
         * unifier is used for the maint. cond. addTerm(lc.getTerm(0));
         * addTerm(l.getTerm(1).capply(newu)); addTerm(lc.getTerm(2));
         * addTerm(lc.getTerm(3)); if (lc.hasAnnot()) setAnnots( lc.getAnnots()
         * );
         */

        this.n = l.n;
        this.s = l.s;
        this.u = u;
        u.compose(l.u);
        this.maintContFromNorm = l.maintContFromNorm;
    }

    // used by copy
    private NormInstance(NormInstance d) {
        super(d);
        this.n = d.n;
        this.s = d.s;
        this.u = d.u;
        this.maintContFromNorm = d.maintContFromNorm;
    }

    /** returns the norms used to create this obligation */
    public INorm getNorm() {
        return n;
    }

    /**
     * returns the unifier used to activate the norm used to create this
     * obligation
     */
    public Unifier getUnifier() {
        return u;
    }

    public Literal getAg() {
        return (Literal) getTerm(0);
    }

    public LogicalFormula getMaitenanceCondition() {
        return (Literal) getTerm(1);
    }

    public LogicalFormula getAim() {
        return (LogicalFormula) getTerm(2);
    }

    /** gets the deadline (in milliseconds) */
    public long getDeadline() {
        try {
            return (long) ((NumberTerm) getTerm(3)).solve();
        } catch (NoValueException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getDoneStr() {
        long toff = -1;
        List<Term> al = getAnnots("done");
        if (!al.isEmpty()) {
            Structure annot = (Structure) al.get(0);
            try {
                toff = (long) ((NumberTerm) annot.getTerm(0)).solve();
                return TimeTerm.toAbsTimeStr(toff);
            } catch (NoValueException e) {
            }
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
        addAnnot(ASSyntax.createStructure("created", new TimeTerm(0, null)));
        addAnnot(ASSyntax.createStructure("norm", new Atom(n.getId()), getUnifierAsTerm()));
    }

    public ListTerm getUnifierAsTerm() {
        ListTerm lf = new ListTermImpl();
        ListTerm tail = lf;
        for (VarTerm k : u) {
            if (!k.isUnnamedVar()) {
                Term vl = u.get(k);
                tail = tail.append(ASSyntax.createList(ASSyntax.createString(k), vl));
            }
        }
        return lf;
    }

    public void setFulfilled() {
        s = State.fulfilled;
        long ttf = System.currentTimeMillis() - getDeadline();
        addAnnot(ASSyntax.createStructure("done", ASSyntax.createNumber(ttf)));
        addAnnot(ASSyntax.createStructure("fulfilled", new TimeTerm(0, null)));
    }

    public void setUnfulfilled() {
        s = State.unfulfilled;
        addAnnot(ASSyntax.createStructure("unfulfilled", new TimeTerm(0, null)));
    }

    public void setInactive() {
        s = State.inactive;
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
        // if (maintContFromNorm)
        // so.append(getNorm().getId());
        // else
        so.append(getMaitenanceCondition());
        so.append("," + getAim() + ",\"");
		if (s == State.active) {
			so.append(TimeTerm.toRelTimeStr(getDeadline()));
		} else { // if (s == State.fulfilled) {
			so.append(TimeTerm.toTimeStamp(getDeadline()));
		}
		so.append("\")");
        if (hasAnnot()) {
            so.append(getAnnots());
        }
        return so.toString();
    }
}
