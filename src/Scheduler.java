import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Priority Scheduler with Sleep, demotion, and probabilistic picking.
 * Matches PCB(UserlandProcess, OS.PriorityType) API you sent.
 */
public class Scheduler {

    // Kernel reads this and calls next.start() after each context switch.
    public PCB currentlyRunning;

    private final Deque<PCB> rtQ  = new ArrayDeque<>();
    private final Deque<PCB> intQ = new ArrayDeque<>();
    private final Deque<PCB> bgQ  = new ArrayDeque<>();

    private static final class SleepEntry implements Comparable<SleepEntry> {
        final PCB pcb;
        final long wakeAtMillis;
        SleepEntry(PCB pcb, long wakeAtMillis) {
            this.pcb = pcb; this.wakeAtMillis = wakeAtMillis;
        }
        @Override public int compareTo(SleepEntry o) {
            return Long.compare(this.wakeAtMillis, o.wakeAtMillis);
        }
    }
    private final PriorityQueue<SleepEntry> sleepers = new PriorityQueue<>();

    private final Random rng = new Random();
    private final Clock clock = Clock.systemUTC();

    // Track why the last switch happened (affects demotion + requeue).
    private enum SwitchReason { PREEMPT_OR_COOPERATE, SLEEP, EXIT }
    private SwitchReason lastReason = SwitchReason.PREEMPT_OR_COOPERATE;

    /** Create a new process with a given priority; returns PID. */
    public int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        PCB pcb = new PCB(up, priority);
        enqueueByPriority(pcb, priority);
        // PID is assigned inside PCB; expose it
        return pcb.pid;
    }

    /** Put the current process to sleep for ms; pick next to run. */
    public void Sleep(int ms) {
        if (currentlyRunning != null) {
            long wakeAt = clock.millis() + Math.max(0, ms);
            sleepers.add(new SleepEntry(currentlyRunning, wakeAt));
            currentlyRunning.sleeping = true;
            // Voluntary block resets timeout streak to avoid demotion
            currentlyRunning.resetTimeoutCounter();
        }
        lastReason = SwitchReason.SLEEP;
        pickNextAndSet();
    }

    /** Return PID of the currently running process, or -1 if none. */
    public int GetPid() {
        return (currentlyRunning == null) ? -1 : currentlyRunning.pid;
    }

    /** Unschedule current permanently; pick another. */
    public void ExitCurrent() {
        if (currentlyRunning != null) {
            currentlyRunning.exiting = true;
        }
        lastReason = SwitchReason.EXIT;
        pickNextAndSet();
    }

    /**
     * Task switch (triggered by OS.SwitchProcess()).
     * Treat as quantum end unless the process voluntarily slept/exited.
     */
    public void SwitchProcess() {
        // Wake sleepers first
        awakenSleepers();

        // Requeue previously running (if applicable)
        if (currentlyRunning != null && lastReason == SwitchReason.PREEMPT_OR_COOPERATE) {
            PCB cur = currentlyRunning;

            // Count demotion only if this was a real timeout (not a voluntary yield/sleep).
            // If you have a timer that marks timeouts, honor it:
            boolean wasTimeout = cur.consumeTimeoutSignal(); // true only if preempted by timer
            if (wasTimeout) {
                // Only RT/INT are demoted
                OS.PriorityType p = cur.getPriority();
                if (p == OS.PriorityType.realtime || p == OS.PriorityType.interactive) {
                    int c = cur.incTimeoutCounterAndGet();
                    if (c > 5) {
                        // Demote one level permanently
                        if (p == OS.PriorityType.realtime) {
                            cur.setPriority(OS.PriorityType.interactive);
                        } else {
                            cur.setPriority(OS.PriorityType.background);
                        }
                        cur.resetTimeoutCounter();
                    }
                }
            } else {
                // Voluntary yield => do not increase streak
                cur.resetTimeoutCounter();
            }

            // Put back on the correct ready queue
            enqueueByPriority(cur, cur.getPriority());
        }

        lastReason = SwitchReason.PREEMPT_OR_COOPERATE;
        pickNextAndSet();
    }

    /* ------------------------ internals ------------------------ */

    private void enqueueByPriority(PCB pcb, OS.PriorityType p) {
        pcb.sleeping = false; // becoming ready
        switch (p) {
            case realtime     -> rtQ .addLast(pcb);
            case interactive  -> intQ.addLast(pcb);
            case background   -> bgQ .addLast(pcb);
        }
    }

    private void awakenSleepers() {
        long now = clock.millis();
        while (!sleepers.isEmpty() && sleepers.peek().wakeAtMillis <= now) {
            PCB pcb = sleepers.poll().pcb;
            if (!pcb.exiting) {
                enqueueByPriority(pcb, pcb.getPriority());
            }
        }
    }

    private void pickNextAndSet() {
        awakenSleepers(); // just-in-time wake before picking

        PCB next = pickNextPCB();
        currentlyRunning = next; // Kernel will .start() this if not null
    }

    private PCB pickNextPCB() {
        boolean hasRT  = !rtQ .isEmpty();
        boolean hasINT = !intQ.isEmpty();
        boolean hasBG  = !bgQ .isEmpty();

        if (hasRT) {
            int roll = rng.nextInt(10); // 0..9
            if (roll < 6 && hasRT)  return rtQ .pollFirst(); // 60% RT
            if (roll < 9 && hasINT) return intQ.pollFirst(); // 30% INT
            if (hasBG)              return bgQ .pollFirst(); // 10% BG
            // Fallbacks if chosen bucket empty
            if (hasRT)  return rtQ .pollFirst();
            if (hasINT) return intQ.pollFirst();
            if (hasBG)  return bgQ .pollFirst();
            return null;
        } else if (hasINT) {
            int roll = rng.nextInt(4); // 0..3
            if (roll < 3 && hasINT) return intQ.pollFirst(); // 75% INT
            if (hasBG)             return bgQ .pollFirst();  // 25% BG
            // Fallbacks
            if (hasINT) return intQ.pollFirst();
            if (hasBG)  return bgQ .pollFirst();
            return null;
        } else {
            // Only BG (or nothing)
            if (hasBG) return bgQ.pollFirst();
            return null;
        }
    }
}
