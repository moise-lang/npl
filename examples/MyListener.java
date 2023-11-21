import jason.asSyntax.Structure;
import npl.NormInstance;
import npl.NormativeListener;

/** very simple normative listener that print messages for normative events */
class MyListener implements NormativeListener {
    @Override
    public void created(NormInstance o) {
        System.out.println("created: "+o);
    }

    @Override
    public void fulfilled(NormInstance o) {
        System.out.println("fulfilled: "+o);
    }

    @Override
    public void unfulfilled(NormInstance o) {
        System.out.println("unfulfilled: "+o);
    }

    @Override
    public void inactive(NormInstance o) {
        System.out.println("inactive: "+o);
    }

    @Override
    public void failure(Structure f) {
        System.out.println("failure: "+f);
    }
}
