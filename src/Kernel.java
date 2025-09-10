public class Kernel extends Process  {
    private final Scheduler scheduler;

    public Kernel() {
        super();
        this.scheduler = new Scheduler();
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void main() {
        while (true) { // Warning on infinite loop is OK...
            if (OS.currentCall != null) {
                switch (OS.currentCall) { // get a job from OS, do it
                    case CreateProcess ->
                            OS.retVal = CreateProcess((UserlandProcess) OS.parameters.get(0), (OS.PriorityType) OS.parameters.get(1));
                    case SwitchProcess -> {
                        SwitchProcess();
                        OS.retVal = Boolean.TRUE;
                    }
                    // Other cases reserved for later assignments...
                }
                // Clear the call marker so OS can detect completion.
                OS.currentCall = null;
            }

            // Now that we have done the work asked of us, start some process then go to sleep.
            if (scheduler.currentlyRunning != null) {
                scheduler.currentlyRunning.start();
            }
            // Only one process runs at a time; kernel yields.
            this.stop();
        }
    }

    private void SwitchProcess() {
        scheduler.SwitchProcess();
    }

    // For assignment 1, you can ignore the priority. We will use that in assignment 2
    private int CreateProcess(UserlandProcess up, OS.PriorityType priority) {
        return scheduler.CreateProcess(up, priority);
    }

    // ---- Stubs for future assignments ----
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
