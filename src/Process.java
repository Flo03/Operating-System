import java.util.concurrent.Semaphore;

public abstract class Process implements Runnable {
    private final Thread thread = new Thread(this, this.getClass().getSimpleName() + "-thread");
    private final Semaphore sem = new Semaphore(0);

    public Process() { thread.start(); }

    public abstract void main();

    public void start() { this.sem.release(); }
    public void stop() { this.sem.drainPermits(); }
    public boolean isStopped() { return this.sem.availablePermits() == 0; }
    public boolean isDone() { return !this.thread.isAlive(); }
    public void requestStop() {} // overridden in userland if needed

    @Override
    public void run() {
        while (true) {
            this.sem.acquireUninterruptibly();
            this.main();
        }
    }

    public void cooperate() {
        this.stop();
        OS.switchProcess();
        this.sem.acquireUninterruptibly();
    }
}
