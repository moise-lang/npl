package npl;

import npl.parser.ParseException;


public class NormativeProgram {

    private String        id = "";
    private String        src = "--no-source--";
    private Scope         root;
    
    public final static String FailFunctor   = "fail";
    public final static String OblFunctor    = "obligation";
    public final static String PerFunctor    = "permission";
    public final static String ProFunctor    = "prohibition";
    
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
        this.root = root;
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        if (root != null)
            out.append("\n"+root.toString());
        return out.toString();
    }
}
