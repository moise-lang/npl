package npl;

import java.util.ConcurrentModificationException;

public class StateTransitionsThread extends StateTransitions implements Runnable {

    //private boolean runUpdate = false;
    private int updateInterval;
    private boolean running = true;

    private Thread myThread;

    public StateTransitionsThread(NPLInterpreter engine, int updateInterval) {
        super(engine);
        this.updateInterval = updateInterval;
    }

    @Override
    public void setUpdateInterval(int miliseconds) {
        updateInterval = miliseconds;
    }

    @Override
    public void start() {
        if (myThread == null) {
            myThread = new Thread(this);
            myThread.start();
        } else {
            engine.logger.warning("cannot start this class thread twice, ignoring.");
        }
    }

    @Override
    public void stop() {
        myThread.interrupt();
        running = false;
    }

    /**
     * update the state of the obligations
     */
    @Override
    synchronized void update() {
        //runUpdate = true;
        notifyAll();
    }

    @Override
    synchronized void deadlineAchieved(NormInstance o) {
        super.deadlineAchieved(o);
        //runUpdate = true;
        notifyAll();
    }

    @Override
    synchronized public void run() {
        boolean concModifExp = false;
        while (running) {
            try {
                if (concModifExp) {
                    myThread.sleep(50);
                    concModifExp = false;
                } else {
                    wait(updateInterval);
                }
                //if (runUpdate) {
                    synchronized (engine.syncTransState) {
                        //runUpdate = false;
                        super.update();
                    }
                //}
            } catch (ConcurrentModificationException e) {
                // sleeps a while and try again
                concModifExp = true;
            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
