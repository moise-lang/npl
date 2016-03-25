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
        interpreter.init();
        interpreter.loadNP(np.getRoot()); 

        // listen events from NPInterpreter
        interpreter.addListener(new MyListener()); 

        // create initial obligations
        interpreter.verifyNorms();
        System.out.println("Active obligations 0: "+ interpreter.getActiveObligations()); 
        
        // change b value to trigger the first norm (n1)
        facts.setBValue(3);
        interpreter.verifyNorms();
        System.out.println("Active obligations 1: "+ interpreter.getActiveObligations());
        
        Thread.sleep(10000); // wait some time to see what happens
        
        // alice fulfills its obl
        facts.setBValue(-1);
        
        Thread.sleep(2000);
        System.exit(0);
    }
    
    /** evaluates b/1 facts */
    class BFacts implements DynamicFactsProvider {
        
        PredicateIndicator b1 = new PredicateIndicator("b", 1);
        
        Literal bInst = ASSyntax.createLiteral("b", ASSyntax.createNumber(1) ); // creates literal b(1)

        @Override
        public boolean isRelevant(PredicateIndicator pi) {
            return pi.equals(b1);
        }

        public void setBValue(int i) {
            bInst = ASSyntax.createLiteral("b", ASSyntax.createNumber(i) );
        }
        
        @Override
        public Iterator<Unifier> consult(Literal l, Unifier u) {
            u = u.clone();
            u.unifies(l, bInst);
            //System.out.println("result of consult "+l+" is "+bInst+" "+u);
            return LogExpr.createUnifIterator(u); 
        }
        
    }

    class MyListener extends DefaultNormativeListener {
        @Override
        public void created(Obligation o) {
            System.out.println("Obligation "+o+" created.");
        }
        
        @Override
        public void fulfilled(Obligation o) {
            System.out.println("Obligation "+o+" fulfilled!");
        }
        
        @Override
        public void unfulfilled(Obligation o) {
            System.out.println("Obligation "+o+" unfulfilled!");
        }
        
        @Override
        public void inactive(Obligation o) {
            System.out.println("Obligation "+o+" is inactive");
        }
        
        @Override
        public void failure(Structure f) {
            System.out.println("failure "+f);
        }
    }
}
