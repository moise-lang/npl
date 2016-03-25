package npl;

import jason.asSyntax.PredicateIndicator;
import npl.parser.ParseException;


public class NormativeProgram {

    private String        id = "";
    private String        src = "--no-source--";
    private Scope         root;
    
    public final static String FailFunctor   = "fail";
    public final static String OblFunctor    = "obligation";
    public final static String FFFunctor     = "fulfilled";
    public final static String UFFFunctor    = "unfulfilled";
    public final static String ActFunctor    = "active";
    public final static String InactFunctor  = "inactive";
    
    public final static PredicateIndicator ACTPI  = new PredicateIndicator(NormativeProgram.ActFunctor,1);
    public final static PredicateIndicator FFPI   = new PredicateIndicator(NormativeProgram.FFFunctor,1);
    public final static PredicateIndicator UFPI   = new PredicateIndicator(NormativeProgram.UFFFunctor,1);
    public final static PredicateIndicator INACPI = new PredicateIndicator(NormativeProgram.InactFunctor,1);
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getSrc() {
        return src;
    }
    public void setSrc(String src) {
        this.src = src;
    }

    public Scope getRoot() {
        return root;
    }
    public void setRoot(Scope root) throws ParseException {
        if (!root.getId().getFunctor().equals("organisation"))
            throw new ParseException("the root scope must be named 'organisation'");
        this.root = root;
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        //out.append("np "+id+" {\n");
        /*
        if (! osf.isEmpty()) {
            out.append("  os {\n");
            for (Literal l: osf) 
                out.append("    "+l+".\n");
            out.append("  }\n");
        }
        */
        if (root != null)
            out.append("\n"+root.toString());
        //out.append("}\n");
        return out.toString();
    }
}
