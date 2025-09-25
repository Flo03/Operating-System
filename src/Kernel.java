public class Kernel extends Process {
    // Private scheduler owned by the kernel (rubric)
    private final Scheduler scheduler;

    public Kernel() {
        super();
        this.scheduler = new Scheduler();
    }

    // Accessor used by OS to stop the running user (mode switch)
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void main() {
        while (true) {
            switch (OS.currentCall) {
                case CreateProcess -> {
                    UserlandProcess up = (UserlandProcess) OS.parameters.get(0);
                    OS.PriorityType p = (OS.PriorityType) OS.parameters.get(1);
                    int pid = scheduler.CreateProcess(up, p);
                    OS.retVal = pid;
                }
                case SwitchProcess -> {
                    scheduler.SwitchProcess();
                    OS.retVal = Boolean.TRUE;
                }
                case Sleep -> {
                    int ms = (int) OS.parameters.get(0);
                    scheduler.Sleep(ms);               // puts current into sleep queue & switches
                    OS.retVal = Boolean.TRUE;
                }
                case GetPID -> {
                    OS.retVal = scheduler.GetPid();    // pid of currently running
                }
                case Exit -> {
                    scheduler.ExitCurrent();           // unschedule current and switch
                    OS.retVal = Boolean.TRUE;
                }
                case NONE -> {
                    if (OS.retVal == null) OS.retVal = Boolean.TRUE;
                }
                default -> {
                    if (OS.retVal == null) OS.retVal = Boolean.TRUE;
                }
            }

            // Mark request handled
            OS.currentCall = OS.CallType.NONE;

            // Hand CPU to chosen userland process, then yield kernel
            PCB next = scheduler.currentlyRunning;   // <-- FIX: PCB, not Process
            if (next != null) {
                next.start();                        // PCB.start() resumes that process
            }
            this.stop();
        }
    }
}
