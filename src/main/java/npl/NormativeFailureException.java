package npl;

import jason.asSyntax.Structure;

/**
 * Exception that represents the violation of some regimentation defined in a NPL program.
 *
 * @author Jomi
 */
public class NormativeFailureException extends Exception {

    Structure fail = null;

    public NormativeFailureException(Structure fail) {
        super("normative failure: "+fail);
        this.fail = fail;
    }

    public Structure getFail() {
        return fail;
    }

    @Override
    public String toString() {
        return "normative failure, details: "+fail.toString();
    }
}
