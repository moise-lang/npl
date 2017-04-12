import java.io.StringReader;

import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.VarTerm;
import junit.framework.TestCase;
import npl.DeonticModality;
import npl.Norm;
import npl.NormativeProgram;
import npl.Scope;
import npl.parser.ParseException;
import npl.parser.nplp;

/** JUnit test case for syntax package */
public class NPLParserTest extends TestCase {

    String program    = ""; // np test { \n"; //os { a1(c,4,\"a\"). a2(b(1,a)). } \n";

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
        assertEquals(3, p.getRoot().getRules().size());
        assertEquals(0, p.getRoot().getNorms().size());
        Scope wp = p.getRoot().getScope(ASSyntax.parseLiteral("group(wp)"));
        assertTrue(wp != null);
        assertEquals(2, wp.getRules().size());
        assertEquals(2, wp.getNorms().size());
        Literal o = p.getRoot().getScope(ASSyntax.parseLiteral("group(wp)")).getNorm("n2").getConsequence();
        assertEquals("(vl(X) & (Y > 10))", o.getTerm(1).toString());
        assertEquals("[norm(n2(a))]", o.getAnnots().toString());
        //System.out.println(p);
        
        Norm n2 = p.getRoot().findScope("group(wp)").getNorm("n2");
        assertNotNull(n2);
        Unifier u = new Unifier();
        u.unifies(new VarTerm("X"), ASSyntax.createNumber(10));
        DeonticModality obl = new DeonticModality(n2.getConsequence(), u, n2);
        assertTrue(obl.toString().startsWith("obligation(bob,(vl(10) & (Y > 10)),(ii(10) & (10 > 5))"));
        System.out.println(obl);
        u = new Unifier();
        u.unifies(new VarTerm("Y"), ASSyntax.createNumber(20));
        obl = obl.capply(u);
        System.out.println(obl);
        assertTrue(obl.toString().startsWith("obligation(bob,(vl(10) & (20 > 10)),(ii(10) & (10 > 5))"));
        
    }
    
    /*
    public void testWP() throws ParseException, Exception {
        NormativeProgram p = new NormativeProgram();
        String src = "examples/writePaper/wp.npl";
        p.setSrc(src);
        new nplp(new FileReader(src)).program(p);
        
        assertEquals(0, p.getRoot().getRules().size());
        assertEquals(0, p.getRoot().getNorms().size());
        Scope wp = p.getRoot().getScope(ASSyntax.parseLiteral("group(wpgroup)"));
        assertTrue(wp != null);

        //System.out.println(p);
        assertEquals(7, wp.getRules().size());
        assertEquals(4, wp.getNorms().size());
    }
    */

}
