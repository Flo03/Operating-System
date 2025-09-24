import java.util.concurrent.Semaphore;


/**
 * Base Process abstraction for both userland and kerneland.
 * Implements cooperative multitasking using a Semaphore and a quantum-expired flag.
 */


public abstract class Process implements Runnable {
    private final Thread thread;
    protected final Semaphore sem = new Semaphore(0);
    private volatile boolean quantumExpired = false;

    public Process() {
        // Each Process is a Runnable with its own thread.
        this.thread = new Thread(this, this.getClass().getSimpleName());
        this.thread.start();
    }

    /** Mark this process to yield on its next cooperate() call. */
    public void requestStop() {
        quantumExpired = true;
    }

    /** Program entry for subclasses. Should typically contain an infinite loop. */
    public abstract void main();

    /** True when the semaphore has zero permits (i.e., currently stopped). */
    public boolean isStopped() {
        return sem.availablePermits() == 0;
    }

    /** True when the underlying Java thread is no longer alive. */
    public boolean isDone() {
        return !thread.isAlive();
    }

    /** Allow this process to run (release a permit). */
    public void start() {
        sem.release();
    }

    /** Stop this process (acquire a permit), blocking until available. */
    public void stop() {
        try {
            sem.acquire();
        } catch (InterruptedException ignored) {}
    }

    /** Thread body: wait to be started, then run the "program" main(). */
    public void run() { // This is called by the Thread - NEVER CALL THIS!!!
        try {
            sem.acquire();
        } catch (InterruptedException ignored) {}
        main();
    }

    /** Called frequently by userland. If quantum expired, switch into the kernel. */
    public void cooperate() {
        if (quantumExpired) {
            quantumExpired = false;
            OS.switchProcess();
        } else {
            Thread.yield();
        }
    }
}
