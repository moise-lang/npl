import jason.asSyntax.ASSyntax;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.parser.nplp;
import util.NPLMonitor;

import java.io.FileInputStream;


/**
 * This example is based on the normative program of file e4.npl
 * and illustrates sanctions.
 *
 */
public class Example4 {

    NormativeProgram  np          = new NormativeProgram(); // where the parser will place the result of parsing (norms, rules, ....)
    NPLInterpreter    interpreter = new NPLInterpreter(); // the NPL interpreter

    public static void main(String[] args) throws Exception {
        new Example4().run();
    }

    public void run() throws Exception {
        // parsing
        nplp parser = new nplp(new FileInputStream("examples/e4.npl"));
        parser.program(np, null);
        System.out.println(np);

        // loads the program into the interpreter
        interpreter.loadNP(np.getRoot());

        // listen events from NPInterpreter
        interpreter.addListener(new MyListener());

        // starts GUI
        NPLMonitor m = new NPLMonitor();
        m.add("example 4", interpreter);

        // verifies if some norm is applicable (none in this example)
        interpreter.verifyNorms();

        // sanctions not triggered until here because there is no c value
        Thread.sleep(5000);

        interpreter.addFact(ASSyntax.parseLiteral("c(10)"));
        // sanctions start to be created

        interpreter.addFact(ASSyntax.parseLiteral("b(0)")); // no more obligations for alice

        for (int i=0; i<10; i++) {
        	Thread.sleep(2000);
        	interpreter.verifyNorms();
        }
        System.exit(0);
    }
}
