import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LogExpr;
import jason.asSyntax.PredicateIndicator;

import java.io.FileInputStream;
import java.util.Iterator;

import util.NPLMonitor;
import npl.DeonticModality;
import npl.DynamicFactsProvider;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.parser.nplp;


/** 
 * Basic example of NPL use.
 *  
 * This example is based on the normative program of file e1.npl 
 * and illustrates obligations.
 *  
 */
public class Example1 {
    
    NormativeProgram  np          = new NormativeProgram(); // where the parser will place the result of parsing (norms, rules, ....) 
    NPLInterpreter    interpreter = new NPLInterpreter(); // the NPL interpreter
    BFacts            facts       = new BFacts(); // the class that evaluates values for predicate b/1

    public static void main(String[] args) throws Exception {
        new Example1().run();
    }
    
    public void run() throws Exception {
        // parsing
        nplp parser = new nplp(new FileInputStream("src/examples/e1.npl"));        
        parser.program(np, facts);
        System.out.println(np);
    
        // loads the program into the interpreter
        interpreter.loadNP(np.getRoot()); 

        // listen events from NPInterpreter
        interpreter.addListener(new MyListener()); 

        // starts GUI
        NPLMonitor m = new NPLMonitor();
        m.add("example 1", interpreter);

        // verifies if some norm is applicable (none in this example)
        interpreter.verifyNorms();
        printObl();
        
        // changes b value to trigger the first norm (n1)
        facts.setBValue(3);
        interpreter.verifyNorms();
        printObl();
        
        Thread.sleep(10000); // wait some time to see what happens
        
        // alice fulfills her obligation
        facts.setBValue(-1);
        Thread.sleep(4000);
        
        // creates obligations for bob and carlos
        facts.setBValue(10);
        interpreter.verifyNorms();        
                
        // disactivate norm for bob removing his state of student
        interpreter.removeFact(ASSyntax.parseLiteral("student(bob,_)"));
        interpreter.verifyNorms();        
        
        Thread.sleep(15000);        
        printObl();
        System.exit(0);
    }
    
    void printObl() {
        System.out.println("Active Obligations:");
        for (DeonticModality o: interpreter.getActiveObligations()) {
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
