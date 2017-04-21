package npl;

import jason.asSyntax.Literal;
import jason.asSyntax.Rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scope {
    private Literal       id;
    private List<Rule>    rules = new ArrayList<Rule>();
    private Scope         father = null;
    private NormativeProgram   program = null;
    private Map<String,INorm>   norms  = new HashMap<String,INorm>();
    private Map<Literal,Scope> scopes = new HashMap<Literal,Scope>();
    
    public Scope(Literal id, NormativeProgram np) {
        this.id = id;
        this.program = np;
    }
    public Literal getId() {
        return id;
    }
    public NormativeProgram getNP() {
        return program;
    }
    
    public void addRule(Rule r) {
        rules.add(r);
    }
    public List<Rule> getRules() {
        return rules;
    }
    
    public void addNorm(INorm n) {
        norms.put(n.getId(),n);
    }
    public INorm removeNorm(String id) {
        return norms.remove(id);
    }
    public Collection<INorm> getNorms() {
        return norms.values();
    }
    public INorm getNorm(String id) {
        return norms.get(id);
    }
    
    public void addScope(Scope s) {
        scopes.put(s.getId(),s);
    }
    public Collection<Scope> getScopes() {
        return scopes.values();
    }
    public Scope getScope(Literal key) {
        return scopes.get(key);
    }
    public Scope findScope(String sid) {
        if (this.id.toString().equals( sid ))
            return this;
        for (Scope s: scopes.values()) {
            Scope r = s.findScope(sid);
            if (r != null)
                return r;
        }
        return null;
    }
    
    

    public Scope getFather() {
        return father;
    }
    public void setFather(Scope s) {
        father = s;
    }
    
    public int getNbFathers() {
        if (father == null)
            return 0;
        else 
            return 1+father.getNbFathers();
    }

    @Override
    public String toString() {
        StringBuilder spaces = new StringBuilder();
        for (int i=0; i<getNbFathers(); i++)
            spaces.append("  ");
        String s = spaces.toString();
        
        StringBuilder out = new StringBuilder();
        out.append(s+"scope "+id+" {\n");
        for (Rule r: rules) 
            out.append(s+"  "+r+".\n");
        for (INorm n: norms.values()) 
            out.append(s+"  "+n+".\n");
        for (Scope sc: scopes.values()) 
            out.append("\n"+sc);
        out.append(s+"}\n");
        return out.toString();
    }
}
