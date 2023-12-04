package npl;

import java.util.Date;

import javax.json.Json;
import javax.json.JsonValue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jason.asSemantics.Unifier;
import jason.asSyntax.DefaultTerm;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;

/** A Jason Term that represent a moment on time */
public class TimeTerm extends DefaultTerm implements NumberTerm {
    private static final long serialVersionUID = -5935273329840616372L;

    Date time; // time == null means 'now'
    final long   t;
    final String unit;

    private TimeTerm() { // for clone
        t = 0;
        unit = null;
    }

    public TimeTerm(Date time) {
        this();
        this.time = time;
    }

    public TimeTerm(long t, String unit) {
        this.t    = t;
        this.unit = unit;
        if (unit == null)
            time = new Date();
        else if (unit.equals("never"))
            time = new Date(Long.MAX_VALUE);
        else if (unit.startsWith("millisecond"))
            time = new Date(t);
        else if (unit.startsWith("second"))
            time = new Date(t * 1000);
        else if  (unit.startsWith("minute"))
            time = new Date(t * 1000 * 60);
        else if  (unit.startsWith("hour"))
            time = new Date(t * 1000 * 60 * 60);
        else if  (unit.startsWith("day"))
            time = new Date(t * 1000 * 60 * 60 * 24);
        else if  (unit.startsWith("year"))
            time = new Date(t * 365 * 1000 * 60 * 60 * 24);
    }

    @Override
    protected int calcHashCode() {
        if (time == null) // now
            return "now".hashCode();
        else
            return time.hashCode();
    }

    @Override
    public Term capply(Unifier u) {
        TimeTerm t = (TimeTerm)clone();
        if (t.time == null) {
            t.time = new Date(); // now is not 'now' anymore, but a fixed point in time
        }
        return t;
    }

    @Override
    public Term clone() {
        if (time == null) { // now
            return new TimeTerm(t,unit);
        } else {
            return new TimeTerm(this.time);
        }
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    public double solve() {
        if (time == null) // now
            return 0; //new Date().getTime();
        else
            return time.getTime();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (! (obj instanceof TimeTerm)) return false;
        TimeTerm o = (TimeTerm)obj;
        return o.solve() == this.solve();
    }

    @Override
    public String toString() {
        if (time == null) // now
            return "`now`";
        else
            return "\""+String.valueOf( toTimeStamp(time.getTime()))+"\""; // enclosed by " to avoid parser error
    }

    @SuppressWarnings("deprecation")
    public static String toTimeStamp(long time) {
        Date t = new Date(time);
        if (t.getYear() > 20000000)
            return "--";
        if (time < 1000000)
            return toTimeSliceStr(time);
        return (1900+t.getYear())+"-"+(t.getMonth()+1)+"-"+t.getDate()+" "+t.getHours()+":"+t.getMinutes()+":"+t.getSeconds();
    }

    public static String toRelativeTimeStr(long time) {
        long elap = time - System.currentTimeMillis();
        String s = "";
        if (elap < 0) {
            s = "-";
            elap = elap * -1;
        }
        long t = 0;
        String u = "milliseconds";
        if ( elap >= (356 * 1000 * 60 * 60 * 24)) {
            u = "years";
            t = elap / (356 * 1000 * 60 * 60 * 24);
            if (t > 30)
                return "--";
        } else if ( elap >= (1000 * 60 * 60 * 24)) {
            u = "days";
            t = elap / (1000 * 60 * 60 * 24);
        } else if (elap >= 1000 * 60 * 60) {
            u = "hours";
            t = elap / (1000 * 60 * 60);
        } else if (elap >= 1000 * 60) {
            u = "minutes";
            t = elap / (1000 * 60);
        } else if (elap >= 1000) {
            u = "seconds";
            t = elap / 1000;
        }
        if (t == 0)
            return "now";
        else
            return s+t+" "+u;
    }
    public static String toTimeSliceStr(long time) {
        String s = "";
        if (time < 0) {
            s = "-";
            time = time * -1;
        }
        long t = time;
        String u = "milliseconds";
        if ( time >= (356 * 1000 * 60 * 60 * 24)) {
            u = "years";
            t = time / (356 * 1000 * 60 * 60 * 24);
            if (t > 30)
                return "--";
        } else if ( time >= (1000 * 60 * 60 * 24)) {
            u = "days";
            t = time / (1000 * 60 * 60 * 24);
        } else if (time >= 1000 * 60 * 60) {
            u = "hours";
            t = time / (1000 * 60 * 60);
        } else if (time >= 1000 * 60) {
            u = "minutes";
            t = time / (1000 * 60);
        } else if (time >= 1000) {
            u = "seconds";
            t = time / 1000;
        }
        return s+t+" "+u;
    }

    public Element getAsDOM(Document document) {
        Element u = (Element) document.createElement("time-term");
        u.appendChild(document.createTextNode(toString()));
        return u;
    }
    
    public JsonValue getAsJson() {
        return Json.createValue( toString() );      
    }

}
