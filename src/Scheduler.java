import OS.PriorityType;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Multi-queue scheduler with Sleep/wakeup support and basic demotion on repeated timeouts.
 * Modified to notify Kernel to close all devices when a process exits.
 */
public class Scheduler {
    public PCB currentlyRunning;
    private final Deque<PCB> rtQ  = new ArrayDeque<>();
    private final Deque<PCB> intQ = new ArrayDeque<>();
    private final Deque<PCB> bgQ  = new ArrayDeque<>();
    private final PriorityQueue<SleepEntry> sleepers = new PriorityQueue<>();
    private final Random rng = new Random();
    private final Clock clock = Clock.systemUTC();
    private SwitchReason lastReason = SwitchReason.PREEMPT_OR_COOPERATE;

    private final Kernel kernel;

    public Scheduler(Kernel kernel) {
        this.kernel = kernel;
    }

    public int CreateProcess(UserlandProcess up, PriorityType priority) {
        PCB pcb = new PCB(up, priority);
        enqueueByPriority(pcb, priority);
        return pcb.pid;
    }

    public void Sleep(int ms) {
        if (currentlyRunning != null) {
            long wakeAt = clock.millis() + Math.max(0L, ms);
            sleepers.add(new SleepEntry(currentlyRunning, wakeAt));
            currentlyRunning.sleeping = true;
            currentlyRunning.resetTimeoutCounter();
        }
        lastReason = SwitchReason.SLEEP;
        pickNextAndSet();
    }

    public int GetPid() {
        return (currentlyRunning == null) ? -1 : currentlyRunning.pid;
    }

    public void ExitCurrent() {
        if (currentlyRunning != null) {
            currentlyRunning.exiting = true;
            // Close its devices right away
            if (kernel != null) kernel.closeAllDevicesFor(currentlyRunning);
        }
        lastReason = SwitchReason.EXIT;
        pickNextAndSet();
    }

    public void SwitchProcess() {
        awakenSleepers();

        if (currentlyRunning != null && lastReason == SwitchReason.PREEMPT_OR_COOPERATE) {
            PCB cur = currentlyRunning;

            // If the running process is exiting or done, do NOT requeue; close if needed
            if (cur.exiting || cur.isDone()) {
                if (kernel != null) kernel.closeAllDevicesFor(cur);
            } else {
                boolean wasTimeout = cur.consumeTimeoutSignal();
                if (wasTimeout) {
                    PriorityType p = cur.getPriority();
                    if (p == PriorityType.realtime || p == PriorityType.interactive) {
                        int c = cur.incTimeoutCounterAndGet();
                        if (c > 5) {
                            if (p == PriorityType.realtime) cur.setPriority(PriorityType.interactive);
                            else cur.setPriority(PriorityType.background);
                            cur.resetTimeoutCounter();
                        }
                    }
                } else {
                    cur.resetTimeoutCounter();
                }
                enqueueByPriority(cur, cur.getPriority());
            }
        }

        lastReason = SwitchReason.PREEMPT_OR_COOPERATE;
        pickNextAndSet();
    }

    private void enqueueByPriority(PCB pcb, PriorityType p) {
        pcb.sleeping = false;
        switch (p) {
            case realtime -> rtQ.addLast(pcb);
            case interactive -> intQ.addLast(pcb);
            case background -> bgQ.addLast(pcb);
        }
    }

    private void awakenSleepers() {
        long now = clock.millis();
        while (!sleepers.isEmpty() && sleepers.peek().wakeAtMillis <= now) {
            PCB pcb = sleepers.poll().pcb;
            if (!pcb.exiting) enqueueByPriority(pcb, pcb.getPriority());
        }
    }

    private void pickNextAndSet() {
        awakenSleepers();
        PCB next = pickNextPCB();
        currentlyRunning = next;
    }

    private PCB pickNextPCB() {
        boolean hasRT = !rtQ.isEmpty();
        boolean hasINT = !intQ.isEmpty();
        boolean hasBG = !bgQ.isEmpty();

        if (hasRT) {
            int roll = rng.nextInt(10);
            if (roll < 6 && hasRT) return rtQ.pollFirst();
            else if (roll < 9 && hasINT) return intQ.pollFirst();
            else if (hasBG) return bgQ.pollFirst();
            else if (hasRT) return rtQ.pollFirst();
            else if (hasINT) return intQ.pollFirst();
            else return hasBG ? bgQ.pollFirst() : null;
        } else if (hasINT) {
            int roll = rng.nextInt(4);
            if (roll < 3 && hasINT) return intQ.pollFirst();
            else if (hasBG) return bgQ.pollFirst();
            else if (hasINT) return intQ.pollFirst();
            else return hasBG ? bgQ.pollFirst() : null;
        } else {
            return hasBG ? bgQ.pollFirst() : null;
        }
    }

    private static final class SleepEntry implements Comparable<SleepEntry> {
        final PCB pcb;
        final long wakeAtMillis;
        SleepEntry(PCB pcb, long wakeAtMillis) { this.pcb = pcb; this.wakeAtMillis = wakeAtMillis; }
        public int compareTo(SleepEntry o) { return Long.compare(this.wakeAtMillis, o.wakeAtMillis); }
    }

    private enum SwitchReason { PREEMPT_OR_COOPERATE, SLEEP, EXIT }
}
