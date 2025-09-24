public class Kernel extends Process {
    // Private scheduler owned by the kernel (required by rubric)
    private final Scheduler scheduler;

    public Kernel() {
        super();
        this.scheduler = new Scheduler();
    }

    // Accessor used by OS to check currently running process for mode switch
    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void main() {
        while (true) {
            // Service the current OS call (soft interrupt)
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
                case NONE -> {
                    // no-op; nothing requested
                    if (OS.retVal == null) {
                        OS.retVal = Boolean.TRUE; // ensure OS can proceed if it was waiting
                    }
                }
                // Other calls are stubs for later assignments
                default -> {
                    if (OS.retVal == null) {
                        OS.retVal = Boolean.TRUE;
                    }
                }
            }

            // Clear the request to explicit NONE so OS can detect completion
            OS.currentCall = OS.CallType.NONE;

            // Hand the CPU to the selected user process (if any), then yield kernel
            if (scheduler.currentlyRunning != null) {
                scheduler.currentlyRunning.start();
            }

            // Only one "process" runs at a time; kernel sleeps until next interrupt
            this.stop();
        }
    }

    // ---- Stubs kept for forward compatibility (Assignment 2+) ----
    private void Sleep(int mills) { }
    private void Exit() { }
    private int GetPid() { return 0; }
    private int Open(String s) { return 0; }
    private void Close(int id) { }
    private byte[] Read(int id, int size) { return null; }
    private void Seek(int id, int to) { }
    private int Write(int id, byte[] data) { return 0; }
    private void SendMessage(/*KernelMessage km*/) { }
    private KernelMessage WaitForMessage() { return null; }
    private int GetPidByName(String name) { return 0; }
    private void GetMapping(int virtualPage) { }
    private int AllocateMemory(int size) { return 0; }
    private boolean FreeMemory(int pointer, int size) { return true; }
    private void FreeAllMemory(PCB currentlyRunning) { }
}
