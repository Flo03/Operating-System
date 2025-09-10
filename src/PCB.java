public class PCB { // Process Control Block
    private static int nextPid = 1;

    public final int pid;
    private final UserlandProcess up;
    private OS.PriorityType priority;

    PCB(UserlandProcess up, OS.PriorityType priority) {
        this.pid = nextPid++;
        this.up = up;
        this.priority = priority;
    }

    public String getName() {
        return up.getClass().getSimpleName();
    }

    OS.PriorityType getPriority() {
        return priority;
    }

    public void requestStop() {
        up.requestStop();
    }

    public void stop() { /* calls userlandprocess’ stop. Loops with Thread.sleep() until ulp.isStopped() is true.  */
        up.stop();
        while (!up.isStopped()) {
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }

    public boolean isDone() { /* calls userlandprocess’ isDone() */
        return up.isDone();
    }

    void start() { /* calls userlandprocess’ start() */
        up.start();
    }

    public void setPriority(OS.PriorityType newPriority) {
        priority = newPriority;
    }
}
