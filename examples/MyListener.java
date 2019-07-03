import jason.asSyntax.Structure;
import npl.DeonticModality;
import npl.NormativeListener;

/** very simple normative listener that print messages for normative events */
class MyListener implements NormativeListener {
    @Override
    public void created(DeonticModality o) {
        System.out.println("created: "+o);
    }

    @Override
    public void fulfilled(DeonticModality o) {
        System.out.println("fulfilled: "+o);
    }

    @Override
    public void unfulfilled(DeonticModality o) {
        System.out.println("unfulfilled: "+o);
    }

    @Override
    public void inactive(DeonticModality o) {
        System.out.println("inactive: "+o);
    }

    @Override
    public void failure(Structure f) {
        System.out.println("failure: "+f);
    }
}
