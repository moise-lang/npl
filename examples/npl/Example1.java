import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LogExpr;
import jason.asSyntax.PredicateIndicator;
import jason.asSyntax.Structure;

import java.io.FileInputStream;
import java.util.Iterator;

import npl.DefaultNormativeListener;
import npl.DynamicFactsProvider;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.Obligation;
import npl.parser.nplp;


/** basic example of use of NPL, this example run the normative program of file e1.npl */
public class Example1 {
    
    NormativeProgram  np          = new NormativeProgram(); // where the parser will place the result of parsing (norms, rules, ....) 
    NPLInterpreter    interpreter = new NPLInterpreter(); // interpreter
    BFacts            facts       = new BFacts(); // the class that evaluates values for predicate b/1

    public static void main(String[] args) throws Exception {
        new Example1().run();
    }
    
    public void run() throws Exception {
        // parsing
        nplp parser = new nplp(new FileInputStream("examples/npl/e1.npl"));        
        parser.program(np, facts);
        System.out.println(np);
    
        // loads the program into the interpreter
        interpreter.setScope(np.getRoot()); 

        // listen events from NPInterpreter
        interpreter.addListener(new MyListener()); 

        // create initial obligations
        interpreter.verifyNorms();
        printObl();
        
        // change b value to trigger the first norm (n1)
        facts.setBValue(3);
        interpreter.verifyNorms();
        printObl();
        
        Thread.sleep(10000); // wait some time to see what happens
        
        // alice fulfills its obl
        facts.setBValue(-1);
        Thread.sleep(4000);
        
        // creates obligations for bob and carlos
        facts.setBValue(10);
        interpreter.verifyNorms();        
        
        
        // unactivate norm for bob removing his state of student
        interpreter.removeFact(ASSyntax.parseLiteral("student(bob,_)"));

        Thread.sleep(5000);        
        printObl();
        System.exit(0);
    }
    
    void printObl() {
        System.out.println("Active Obligations:");
        for (Obligation o: interpreter.getActiveObligations()) {
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

    class MyListener extends DefaultNormativeListener {
        @Override
        public void created(Obligation o) {
            System.out.println("Obligation created: "+o);
        }
        
        @Override
        public void fulfilled(Obligation o) {
            System.out.println("Obligation fulfilled: "+o);
        }
        
        @Override
        public void unfulfilled(Obligation o) {
            System.out.println("Obligation unfulfilled: "+o);
        }
        
        @Override
        public void inactive(Obligation o) {
            System.out.println("Obligation inactive: "+o);
        }
        
        @Override
        public void failure(Structure f) {
            System.out.println("failure: "+f);
        }
    }
}
