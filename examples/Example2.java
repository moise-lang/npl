import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LogExpr;
import jason.asSyntax.PredicateIndicator;

import java.io.FileInputStream;
import java.util.Iterator;

import npl.DeonticModality;
import npl.DynamicFactsProvider;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.parser.nplp;
import util.NPLMonitor;


/** 
 * Basic example of NPL use.
 *  
 * This example is based on the normative program of file e2.npl 
 * and illustrates permissions and prohibition.
 *  
 */
public class Example2 {
    
    NormativeProgram  np          = new NormativeProgram(); // where the parser will place the result of parsing (norms, rules, ....) 
    NPLInterpreter    interpreter = new NPLInterpreter(); // the NPL interpreter
    BFacts            facts       = new BFacts(); // the class that evaluates values for predicate b/1

    public static void main(String[] args) throws Exception {
        new Example2().run();
    }
    
    public void run() throws Exception {
        // parsing
        nplp parser = new nplp(new FileInputStream("examples/e2.npl"));        
        parser.program(np, facts);
        System.out.println(np);
    
        // loads the program into the interpreter
        interpreter.loadNP(np.getRoot()); 

        // listen events from NPInterpreter
        interpreter.addListener(new MyListener()); 

        // starts GUI
        NPLMonitor m = new NPLMonitor();
        m.add("example 2", interpreter);
        
        // verifies if some norm is applicable (none in this example)
        interpreter.verifyNorms();
        printState();
        
        // changes b value to trigger the first norm (n1)
        facts.setBValue(3);
        interpreter.verifyNorms();
        printState();
        
        Thread.sleep(5000); // wait some time to see what happens
        
        // deactivate permission for alice
        interpreter.removeFact(ASSyntax.parseLiteral("a(2)"));
        interpreter.verifyNorms();        

        // bob violates the prohibition
        interpreter.addFact(ASSyntax.parseLiteral("a(50)"));
        interpreter.addFact(ASSyntax.parseLiteral("setter(a,bob)")); // it was bob that defined the value of a
        interpreter.verifyNorms();        
        
        // deactivate the prohibitions
        Thread.sleep(1000);        
        interpreter.removeFact(ASSyntax.parseLiteral("student(_,_)"));
        interpreter.removeFact(ASSyntax.parseLiteral("student(_,_)"));
        interpreter.verifyNorms();        

        printState();
        for (int i=0; i<40; i++) {
        	Thread.sleep(1000);   
        	interpreter.verifyNorms();
        }
        System.exit(0);
    }
    
    void printState() {
        System.out.println("Active:");
        for (DeonticModality o: interpreter.getActive()) {
            System.out.println("  "+o);
        }        
    }
    
    /** evaluates b/1 facts (without translating it to a logical literal) */
    class BFacts implements DynamicFactsProvider {
        
        PredicateIndicator b1 = new PredicateIndicator("b", 1);
        int b = 1;

        @Override
        public boolean isRelevant(PredicateIndicator pi) {
            return pi.equals(b1);
        }

        public void setBValue(int i) {
            b = i;
        }
        
        @Override
        public Iterator<Unifier> consult(Literal l, Unifier u) {
            u.unifies(l.getTerm(0), ASSyntax.createNumber(b));
            return LogExpr.createUnifIterator(u); 
        }
    }

}
