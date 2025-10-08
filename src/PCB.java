import java.util.Arrays;

public class PCB {
    private static int nextPid = 1;

    public final int pid;
    private final UserlandProcess up;
    private OS.PriorityType priority;

    // ----- scheduling state (supports demotion/timeouts & sleep) -----
    private int consecutiveTimeouts = 0;
    private volatile boolean timeoutSignaled = false;
    boolean exiting = false;
    boolean sleeping = false;

    // ----- device table (maps user-visible fd -> VFS id) -----
    private final int[] fds = new int[10];

    PCB(UserlandProcess up, OS.PriorityType priority) {
        this.pid = nextPid++;
        this.up = up;
        this.priority = priority;
        Arrays.fill(fds, -1);
    }

    public String getName() { return up.getClass().getSimpleName(); }
    OS.PriorityType getPriority() { return priority; }
    public void setPriority(OS.PriorityType newPriority) { priority = newPriority; }

    public void markTimeoutSignal() { timeoutSignaled = true; }
    public boolean consumeTimeoutSignal() {
        boolean was = timeoutSignaled; timeoutSignaled = false; return was;
    }
    public void resetTimeoutCounter() { consecutiveTimeouts = 0; }
    public int incTimeoutCounterAndGet() { return ++consecutiveTimeouts; }

    public void requestStop() { up.requestStop(); }

    public void stop() {
        up.stop();
        while (!up.isStopped()) {
            try { Thread.sleep(1L); } catch (InterruptedException ignored) {}
        }
    }

    public boolean isDone() { return up.isDone(); }
    void start() { up.start(); }

    // ----- device helpers -----
    public int[] getFdTable() { return fds; }
    public int allocUserFd() {
        for (int i = 0; i < fds.length; i++) if (fds[i] == -1) return i;
        return -1;
    }
    public void setUserFd(int idx, int vfsId) { fds[idx] = vfsId; }
    public int getVfsIdFor(int userFd) {
        return (userFd >= 0 && userFd < fds.length) ? fds[userFd] : -1;
    }
    public void clearUserFd(int userFd) {
        if (userFd >= 0 && userFd < fds.length) fds[userFd] = -1;
    }
}
