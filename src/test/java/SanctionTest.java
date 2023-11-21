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
                "   sanction s1(M): empty(M)       -> stopMachine(M).\n" +
                "   sanction s2(A,M): full(M)      -> jail(A).\n" +
                "   sanction s3 -> stopAll.\n" +
                "   norm n1: a -> obligation(a,b,c,d)\n " +
                "        if unfulfilled: s3, s1(m1) \n" +
                "        if fulfilled: s1(a), s2(alice,m1) \n" +
                "        if inactive: s3." +
                "";
        program = program + "  }";
    }

    public void testParser() throws ParseException, Exception {

        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);
        assertEquals(3, p.getRoot().getSanctionRules().size());
        //System.out.println( p.getRoot().getSanctionRule("s1"));
        assertEquals("sanction s1(M): empty(M) -> stopMachine(M)", p.getRoot().getSanctionRule("s1").toString());
        //System.out.println( p.getRoot().getSanctionRule("s2"));
        assertEquals("sanction s2(A,M): full(M) -> jail(A)", p.getRoot().getSanctionRule("s2").toString());
        assertEquals("sanction s3 -> stopAll", p.getRoot().getSanctionRule("s3").toString());

        //System.out.println(p);
        assertEquals("norm n1: a -> obligation(a,b,c,d) if fulfilled: s1(a), s2(alice,m1) if unfulfilled: s3, s1(m1) if inactive: s3",
                p.getRoot().getNorm("n1").toString());
    }
}
