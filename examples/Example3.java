import java.io.StringReader;
import java.util.Iterator;

import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.LogExpr;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.PredicateIndicator;
import npl.DynamicFactsProvider;
import npl.NPLInterpreter;
import npl.NormativeProgram;
import npl.parser.ParseException;
import npl.parser.nplp;


/**
 * Basic example of NPL use.
 *
 * This example illustrates consults to NPL interpreter
 *
 */
public class Example3 {

    NormativeProgram  np          = new NormativeProgram(); // where the parser will place the result of parsing (norms, rules, ....)
    NPLInterpreter    interpreter = new NPLInterpreter(); // the NPL interpreter
    BFacts            facts       = new BFacts(); // the class that evaluates values for predicate b/1

    public static void main(String[] args) throws Exception {
        new Example3().run();
    }

    public void run() throws Exception {
        // parsing
        nplp parser = new nplp(new StringReader(
        		"scope main {\n" + 
        		"    a(2).\n" + 
        		"    student(bob,2).\n" + 
        		"    student(carlos,3).\n" + 
        		"}"));
        parser.program(np, facts);
        System.out.println(np);

        // loads the program into the interpreter
        interpreter.loadNP(np.getRoot());

        // changes b value to trigger the first norm (n1)
        facts.setBValue(2);
        interpreter.verifyNorms();

        // consult NPL
        testFormula("student(bob,2)");
        testFormula("student(S,V)");
        testFormula("b(V) & student(S,V)");
        
        System.exit(0);
    }
    
    void testFormula(String f) throws ParseException {
    	nplp parser = new nplp(new StringReader(f));
    	parser.setDFP(facts);
        LogicalFormula formula = (LogicalFormula)parser.log_expr();
        System.out.println("Formula: "+formula+" = "+interpreter.holds(formula));
        
        // solutions
        Iterator<Unifier> i = formula.logicalConsequence(interpreter.getAg(), new Unifier());
        while (i.hasNext()) {
        	System.out.println("    Solution: "+i.next());
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
