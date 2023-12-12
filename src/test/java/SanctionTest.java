import jason.asSyntax.ASSyntax;
import junit.framework.TestCase;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.StateTransitions;
import npl.parser.nplp;

import java.io.StringReader;

/** JUnit test case for syntax package */
public class SanctionTest extends TestCase {

    String program = "";

    @Override
    protected void setUp()  {
        program = program + "  scope organisation(xx) { ";
        program = program +
                "   sanction-rule s1(M): empty(M,A)     -> sanction(A,stopMachine).\n" +
                "   sanction-rule s2(A,M): full(M)      -> sanction(A,jail).\n" +
                "   sanction-rule s3 -> sanction(all,stopAll).\n" +
                //"   sanction-rule s4 : b -> obligation(bob,true,fine,`now`)\n " +
                //"        if unfulfilled: s5 \n" +
                //"        if fulfilled: s1(a), s2(alice,M).\n" +
                //"   sanction-rule s5 -> holdAll.\n" +
                "   norm n1: f1(M) -> obligation(a,n1,c,`now`)\n " +
                "        if unfulfilled: s3, s1(M) \n" +
                "        if fulfilled: s1(a), s2(alice,M) \n" +
                "        if inactive: s3." +
                //"   norm n2: f2 -> obligation(a,n2,c,`100 milliseconds`)\n " +
                //"        if unfulfilled: s4." +
                "";
        program = program + "  }";
    }

    public void testParser() throws Exception {
        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);
        assertEquals(3, p.getRoot().getSanctionRules().size());
        //System.out.println( p.getRoot().getSanctionRule("s1"));
        assertTrue(p.getRoot().getSanctionRules().toString().contains("sanction-rule s1(M): empty(M,A) -> sanction(A,stopMachine)"));
        //System.out.println( p.getRoot().getSanctionRule("s2"));
        assertTrue(p.getRoot().getSanctionRules().toString().contains("sanction-rule s2(A,M): full(M) -> sanction(A,jail)"));
        assertTrue(p.getRoot().getSanctionRules().toString().contains("sanction-rule s3 -> sanction(all,stopAll)"));
        //assertEquals("sanction-rule s4: b -> obligation(bob,true,fine,`now`) if fulfilled: s1(a), s2(alice,M) if unfulfilled: s5", p.getRoot().getSanctionRule("s4").toString());

        //System.out.println(p);
        assertEquals(
                "norm n1: f1(M) -> obligation(a,f1(M),c,`now`) if fulfilled: s1(a), s2(alice,M) if unfulfilled: s3, s1(M) if inactive: s3",
                p.getRoot().getNorm("n1").toString());
    }

    public void testSanctionCreation1() throws Exception {
        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);

        NPLInterpreter interpreter = new NPLInterpreter();
        interpreter.setStateManager(new StateTransitions(interpreter));

        interpreter.loadNP(p.getRoot());
        interpreter.addFact(ASSyntax.parseLiteral("f1(machine1)"));
        interpreter.addFact(ASSyntax.parseLiteral("empty(machine1,m1)"));
        interpreter.addFact(ASSyntax.parseLiteral("empty(machine1,m2)"));
        interpreter.verifyNorms();

        //System.out.println("  **"+interpreter.getFacts());
        assertTrue(interpreter.getFacts().toString().contains("sanction(m1,stopMachine)["));
        assertTrue(interpreter.getFacts().toString().contains("sanction(m2,stopMachine)["));
        assertTrue(interpreter.getFacts().toString().contains("sanction(all,stopAll)["));
    }

    /*public void testSanctionCreation2() throws Exception {
        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);

        NPLInterpreter interpreter = new NPLInterpreter();
        interpreter.setStateManager(new StateTransitions(interpreter));
        interpreter.loadNP(p.getRoot());
        interpreter.addFact(ASSyntax.parseLiteral("f2"));
        interpreter.addFact(ASSyntax.parseLiteral("b"));
        interpreter.verifyNorms();
        Thread.sleep(300);
        interpreter.verifyNorms();
        //System.out.println("  "+interpreter.getActiveObligations());
        //System.out.println("  "+interpreter.getFacts());
        //assertTrue(interpreter.getFacts().toString().contains("obligation(bob,true,fine"));
        assertTrue(interpreter.getFacts().toString().contains("sanction(holdAll["));
    }*/
}
