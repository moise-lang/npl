import java.io.StringReader;

import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.VarTerm;
import junit.framework.TestCase;
import npl.NormInstance;
import npl.INorm;
import npl.NormativeProgram;
import npl.Scope;
import npl.parser.ParseException;
import npl.parser.nplp;

/** JUnit test case for syntax package */
public class NPLParserTest extends TestCase {

    String program = "";

    @Override
    protected void setUp() throws Exception {
        program = program + "  scope organisation(xx) { ";
        program = program + "    orgname(wp).";
        program = program + "    a :- b & c. ";
        program = program + "    a :- d & not c. \n";
        program = program + "    scope group(wp) { ";
        program = program + "      gr(wp,editor).";
        program = program + "      wf :- vl(X) & X>10. ";
        program = program + "      norm n1 : not wf       -> fail(n1(bla)). ";
        program = program + "      norm n2 : vl(X) & Y>10 -> obligation(bob,n2(a),ii(X) & X>5, `now`). ";
        program = program + "    } ";
        program = program + "  }";
    }

    public void testBasic1() throws ParseException, Exception {

        NormativeProgram p = new NormativeProgram();
        p.setSrc("test1");
        new nplp(new StringReader(program)).program(p, null);
        assertEquals(3, p.getRoot().getInferenceRules().size());
        assertEquals(0, p.getRoot().getNorms().size());
        Scope wp = p.getRoot().getScope(ASSyntax.parseLiteral("group(wp)"));
        assertTrue(wp != null);
        assertEquals(2, wp.getInferenceRules().size());
        assertEquals(2, wp.getNorms().size());
        Literal o = p.getRoot().getScope(ASSyntax.parseLiteral("group(wp)")).getNorm("n2").getConsequence();
        assertEquals("(vl(X) & (Y > 10))", o.getTerm(1).toString());

        INorm n2 = p.getRoot().findScope("group(wp)").getNorm("n2");
        assertNotNull(n2);
        Unifier u = new Unifier();
        u.unifies(new VarTerm("X"), ASSyntax.createNumber(10));
        NormInstance obl = new NormInstance(n2.getConsequence(), u, n2);
        assertTrue(obl.toString().startsWith("obligation(bob,(vl(10) & (Y > 10)),(ii(10) & (10 > 5))"));
        u = new Unifier();
        u.unifies(new VarTerm("Y"), ASSyntax.createNumber(20));
        obl = obl.capply(u);
        assertTrue(obl.toString().startsWith("obligation(bob,(vl(10) & (20 > 10)),(ii(10) & (10 > 5))"));
        assertFalse(obl.toString().contains("norm(n2,[[\"Y\",20],[\"X\",10]])")
                || obl.toString().contains("norm(n2,[[\"X\",10],[\"Y\",20]])"));
        obl.setActive();
        assertTrue(obl.toString().contains("norm(n2,[[\"Y\",20],[\"X\",10]])")
                || obl.toString().contains("norm(n2,[[\"X\",10],[\"Y\",20]])"));
    }

    public void testStefanoBug() throws ParseException, Exception {

        NormativeProgram p = new NormativeProgram();
        p.setSrc("test1");
        new nplp(new StringReader("scope main {\n" +
                "   notificationPolicy(npWindowDelay,windows_fitted,(scheme_id(S) & unfulfilled(obligation(_,_,done(S,windows_fitted,_),_)))).\n" +
                "}")).program(p, null);
        assertEquals(0, p.getRoot().getNorms().size());
    }
}
