import jason.asSyntax.ASSyntax;

import java.io.StringReader;

import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.parser.nplp;
import util.NPLMonitor;


/** 
 * Basic example of NPL use.
 *  
 * This example creates an obligation and then fulfills it 
 * and illustrates obligations. 
 *  
 */
public class Example0 {
    
    public static void main(String[] args) throws Exception {
        NormativeProgram  np          = new NormativeProgram(); // where the parser will place the result of parsing (norms, rules, ....) 
        NPLInterpreter    interpreter = new NPLInterpreter(); // the NPL interpreter

        // parsing (from a string)
        nplp parser = new nplp(new StringReader(
                  "scope ex0 {\n"
                + "   norm n1: a(X) & X > 5 -> obligation(bob,true,b(10),`now`+`1 minute`)."
                + "}"));        
        parser.program(np, null);
    
        // loads the program into the interpreter
        interpreter.setScope(np.getRoot()); 

        // starts GUI
        NPLMonitor m = new NPLMonitor();
        m.add("example 0", interpreter);

        // add some fats that trigger the norm
        interpreter.addFact(ASSyntax.parseLiteral("a(10)"));

        // verifies if some norm is applicable
        interpreter.verifyNorms();
        
        // removes the fact that triggered the norm
        interpreter.removeFact(ASSyntax.parseLiteral("a(10)"));

        Thread.sleep(5000);

        // simulates that bob has fulfilled the norm 
        interpreter.addFact(ASSyntax.parseLiteral("b(10)"));
        interpreter.verifyNorms();
        Thread.sleep(2000);

        // triggers it again
        interpreter.addFact(ASSyntax.parseLiteral("a(20)"));
        interpreter.removeFact(ASSyntax.parseLiteral("b(10)")); // we need to remove it, otherwise the norms is already fulfilled
        interpreter.verifyNorms();

        // since the activation condition remains, new obligations will be created for bob as soon as the previous becomes unfulfilled
    }
    
}
