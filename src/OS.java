import java.util.ArrayList;
import java.util.List;

public class OS {
    // Single kernel instance
    private static Kernel ki;

    // Shared call area (soft-interrupt "note")
    public static final List<Object> parameters = new ArrayList<>();
    public static volatile Object retVal;

    public enum CallType {
        NONE,             // explicit "no request pending"
        SwitchProcess,
        SendMessage, Open, Close, Read, Seek, Write,
        GetMapping, CreateProcess, Sleep, GetPID,
        AllocateMemory, FreeMemory, GetPIDByName, WaitForMessage, Exit
    }
    public static volatile CallType currentCall = CallType.NONE;

    // Bridge into kernelland: start kernel, stop running user (if any), wait for completion
    private static void startTheKernel() {
        if (ki == null) {
            ki = new Kernel();
        }

        // New request in flight
        retVal = null;

        // Enter privileged mode
        ki.start();

        // Stop currently running user process, if one exists
        Scheduler s = ki.getScheduler(); // accessor allowed per spec note
        if (s.currentlyRunning != null) {
            s.currentlyRunning.stop();
        }

        // Cold start / general wait: block until kernel sets a return value and clears the call
        while (retVal == null || currentCall != CallType.NONE) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) { }
        }
    }

    // ---- Public OS "syscalls" (userland entry points) ----

    public static void switchProcess() {
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
    }

    public static void Startup(UserlandProcess init) {
        // Create the kernel, then create initial processes through the normal call path
        if (ki == null) {
            ki = new Kernel();
        }
        CreateProcess(init, PriorityType.interactive);
        CreateProcess(new IdleProcess(), PriorityType.background);
    }

    public enum PriorityType { realtime, interactive, background }

    public static int CreateProcess(UserlandProcess up) {
        return CreateProcess(up, PriorityType.interactive);
    }

    // For assignment 1, priority is ignored by the scheduler (used in later assignments)
    public static int CreateProcess(UserlandProcess up, PriorityType priority) {
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        return (int) retVal;
    }

    // ---- Reserved stubs for later assignments ----
    public static int GetPID() {
        parameters.clear();
        currentCall = CallType.GetPID;
        startTheKernel();
        return (int) retVal;
    }

    public static void Exit() {
        parameters.clear();
        currentCall = CallType.Exit;
        startTheKernel();
    }

    public static void Sleep(int mills) {
        parameters.clear();
        parameters.add(mills);
        currentCall = CallType.Sleep;
        startTheKernel();
    }

    public static int Open(String s) { return 0; }
    public static void Close(int id) { }
    public static byte[] Read(int id, int size) { return null; }
    public static void Seek(int id, int to) { }
    public static int Write(int id, byte[] data) { return 0; }
    public static void SendMessage(KernelMessage km) { }
    public static KernelMessage WaitForMessage() { return null; }
    public static int GetPidByName(String name) { return 0; }
    public static void GetMapping(int virtualPage) { }
    public static int AllocateMemory(int size) { return 0; }
    public static boolean FreeMemory(int pointer, int size) { return false; }

    // Package-private accessor so Kernel can be tested if needed (optional)
    static Kernel getKernel() { return ki; }
}
