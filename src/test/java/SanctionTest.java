import junit.framework.TestCase;
import npl.NormativeProgram;
import npl.parser.ParseException;
import npl.parser.nplp;

import java.io.StringReader;

/** JUnit test case for syntax package */
public class SanctionTest extends TestCase {

    String program = "";

    @Override
    protected void setUp() throws Exception {
        program = program + "  scope organisation(xx) { ";
        program = program +
                "   sanction s1(A): empty(M1)       -> stopMachine(M1). " +
                "   sanction s2: full(M1)           -> jail(alice). ";
        program = program + "  }";
    }

    public void testParser() throws ParseException, Exception {

        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);
        assertEquals(2, p.getRoot().getSanctionRules().size());
        //System.out.println( p.getRoot().getSanctionRule("s1"));
        assertEquals("sanction s1(A): empty(M1) -> stopMachine(M1)", p.getRoot().getSanctionRule("s1").toString());
        //System.out.println( p.getRoot().getSanctionRule("s2"));
        assertEquals("sanction s2: full(M1) -> jail(alice)", p.getRoot().getSanctionRule("s2").toString());
    }
}
