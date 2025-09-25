import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable {
    private final Thread thread;
    private final Semaphore sem;

    public Process() {
        this.sem = new Semaphore(0);               // start blocked until scheduled
        this.thread = new Thread(this, getClass().getSimpleName() + "-thread");
        this.thread.start();                       // thread waits on sem until scheduler starts it
    }

    // Userland code implements this (usually an infinite loop that calls cooperate()).
    public abstract void main();

    // --- Scheduler control primitives ---

    // Allow this process to run (release exactly one permit).
    public void start() {
        sem.release();
    }

    // Prevent further immediate runs (ensure 0 permits).
    public void stop() {
        sem.drainPermits();
    }

    // Optional hooks/queries
    public boolean isStopped() { return sem.availablePermits() == 0; }
    public boolean isDone()    { return !thread.isAlive(); }

    // Optional preemption hook used by PCB; no-op in this cooperative design.
    public void requestStop() {
        // intentionally empty
    }

    // Thread entrypoint: wait until scheduled, then run the process code.
    @Override
    public void run() {
        while (true) {
            sem.acquireUninterruptibly();  // wait until the scheduler starts us
            main();                         // user code (will call cooperate() repeatedly)
        }
    }

    // --- Cooperative yield ---
    // ALWAYS yield: block this process, trap to kernel, and wait to be rescheduled.
    public void cooperate() {
        // 1) Ensure we won't keep running until explicitly scheduled again
        stop();
        // 2) Enter kernel to perform a context switch
        OS.switchProcess();
        // 3) Block here until the scheduler picks us again
        sem.acquireUninterruptibly();
    }
}
