package npl;

import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.asSyntax.Literal;
import jason.mas2j.AgentParameters;
import jason.runtime.Settings;
import npl.parser.nplp;

import java.io.FileReader;
import java.util.Collection;

/** agent that has a Normative Reasoning Module in its mind */
class NormativeAg extends Agent {

    protected NPLInterpreter interpreter = new NPLInterpreter();
    private NormativeProgram program;

    @Override
    public void initAg() {
        super.initAg();
        try {
            var agC = ((AgentParameters)getTS().getSettings().getUserParameters().get(Settings.PROJECT_PARAMETER)).agClass;
            if (!agC.getParameters().isEmpty()) {
                var nplFileName = agC.getParameters().iterator().next();
                nplFileName = nplFileName.substring(1,nplFileName.length() - 2);
                logger.info("*** loading norms from "+nplFileName);

                program = new NormativeProgram();
                new nplp(new FileReader(nplFileName)).program(program, null);
            }
            interpreter.setStateManager(new StateTransitions(interpreter));
            interpreter.setAg(this);
            interpreter.init();
            interpreter.loadNP(program.getRoot());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetNPL() {
        interpreter.clearFacts();
    }

    @Override
    public void stopAg() {
        super.stopAg();
        interpreter.stop();
    }

    @Override
    public int buf(Collection<Literal> percepts) {
        var r = super.buf(percepts);
        try {
            interpreter.verifyNorms();
        } catch (NormativeFailureException e) {
            e.printStackTrace();
        }
        return r;
    }

    @Override
    public Agent cloneInto(AgArch arch, Agent a) {
        var newAg = super.cloneInto(arch, a);
        if (newAg instanceof NormativeAg) {
            ((NormativeAg)newAg).interpreter.setAllActivatedNorms( interpreter.getActivatedNorms() );
        }
        return newAg;
    }
}
