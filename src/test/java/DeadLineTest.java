import jason.asSyntax.ASSyntax;
import junit.framework.TestCase;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.parser.nplp;

import java.io.StringReader;

/** JUnit test case for syntax package */
public class DeadLineTest extends TestCase {

    String program = "";

    @Override
    protected void setUp() throws Exception {
        program = program + "  scope organisation(xx) { ";
        program = program +
                "   norm n1: f1(M) -> obligation(a,n1,c,`now`+`2 seconds`)." +  // deadline as time v 0.5
                "   norm n2: f2(M) -> obligation(b,n2,d,open(door))." + // deadline as general cond
                "   norm n3: f1(M) -> obligation(c,n3,c,`2 seconds`)." +  // deadline as time v 0.6 (without now)
                "";
        program = program + "  }";
    }

    public void testDeadLineParser() throws Exception {
        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);

        NPLInterpreter interpreter = new NPLInterpreter();
        interpreter.loadNP(p.getRoot());
        interpreter.addFact(ASSyntax.parseLiteral("f1(machine1)"));
        interpreter.addFact(ASSyntax.parseLiteral("f2(machine1)"));
        var newObl = interpreter.verifyNorms();
        var nis = newObl.iterator();
        assertEquals(3, newObl.size());
        var ni1 = nis.next(); // first instance
        assertTrue(ni1.getTimeDeadline() > 0);
        assertTrue(ni1.getStateDeadline() == null);

        var ni2 = nis.next(); // second instance
        assertTrue(ni2.getTimeDeadline() < 0);
        assertTrue(ni2.getStateDeadline() != null);

        var ni3 = nis.next(); // third instance
        assertTrue(ni3.getTimeDeadline() > 1000000);
        System.out.println(ni3.getTimeDeadline());
        assertTrue(ni3.getStateDeadline() == null);

    }

    public void testDeadLineCond() throws Exception {
        NormativeProgram p = new NormativeProgram();
        new nplp(new StringReader(program)).program(p, null);

        NPLInterpreter interpreter = new NPLInterpreter();
        interpreter.loadNP(p.getRoot());
        interpreter.addFact(ASSyntax.parseLiteral("f1(machine1)"));
        interpreter.addFact(ASSyntax.parseLiteral("f2(machine1)"));
        interpreter.verifyNorms();

        var active = interpreter.getActiveObligations();
        assertEquals(3,active.size());

        // after 2 seconds, two unfulfilled
        Thread.sleep(3000);
        var unfulfilled = interpreter.getUnFulfilled();
        //System.out.println("U: "+unfulfilled);
        assertEquals(2,unfulfilled.size());
        assertTrue(unfulfilled.toString().contains("obligation(c,f1(machine1),c"));
        assertTrue(unfulfilled.toString().contains("obligation(a,f1(machine1),c"));

        // after open(door), 3 unfulfilled
        interpreter.addFact(ASSyntax.parseLiteral("open(door)"));
        interpreter.verifyNorms();
        Thread.sleep(500); // wait the thread to process transitions

        unfulfilled = interpreter.getUnFulfilled();
        //System.out.println("U: "+unfulfilled.size());
        assertTrue(unfulfilled.size()>2);
        assertTrue(unfulfilled.toString().contains("obligation(b,f2(machine1),d,open(door)"));

    }

}
