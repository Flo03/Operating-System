import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

/**  scheduler with a 250ms quantum simulated by a Timer. */
public class Scheduler {
    private final LinkedList<PCB> ready = new LinkedList<>();
    private final Timer timer;
    public PCB currentlyRunning = null;

    public Scheduler() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentlyRunning != null) {
                    currentlyRunning.requestStop();
                }
            }
        }, 250, 250);
    }

    public int CreateProcess(UserlandProcess up, OS.PriorityType p) {
        PCB pcb = new PCB(up, p);
        ready.addLast(pcb);
        if (currentlyRunning == null) {
            SwitchProcess();
        }
        return pcb.pid;
    }

    public void SwitchProcess() {
        // Put the currently running process back into the ready queue if it is still alive

        if (currentlyRunning != null && !currentlyRunning.isDone()) {
            ready.addLast(currentlyRunning);
        }
        currentlyRunning = null;

        // Keep taking from the ready queue until we find a process that is not done, or the queue becomes empty

        while (!ready.isEmpty()) {
            PCB next = ready.removeFirst();
            if (!next.isDone()) {
                currentlyRunning = next;
                break;
            }
        }
    }
}
