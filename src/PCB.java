public class PCB { // Process Control Block
    private static int nextPid = 1;

    public final int pid;
    private final UserlandProcess up;
    private OS.PriorityType priority;

    // For demotion: count consecutive quantum timeouts
    private int consecutiveTimeouts = 0;

    // Set by scheduler's timer when it requests a quantum stop
    private volatile boolean timeoutSignaled = false;

    // State flags for scheduling decisions
    boolean exiting = false;
    boolean sleeping = false;

    PCB(UserlandProcess up, OS.PriorityType priority) {
        this.pid = nextPid++;
        this.up = up;
        this.priority = priority;
    }

    public String getName() {
        return up.getClass().getSimpleName();
    }

    OS.PriorityType getPriority() { return priority; }

    public void setPriority(OS.PriorityType newPriority) {
        this.priority = newPriority;
    }

    public void markTimeoutSignal() {
        timeoutSignaled = true;
    }

    /** Returns true if the last yield was due to timeout; resets the signal. */
    public boolean consumeTimeoutSignal() {
        boolean was = timeoutSignaled;
        timeoutSignaled = false;
        return was;
    }

    public void resetTimeoutCounter() {
        consecutiveTimeouts = 0;
    }

    public int incTimeoutCounterAndGet() {
        return ++consecutiveTimeouts;
    }

    public void requestStop() { up.requestStop(); }
    public void stop() {
        up.stop();
        while (!up.isStopped()) {
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }
    public boolean isDone() { return up.isDone(); }
    void start() { up.start(); }
}
