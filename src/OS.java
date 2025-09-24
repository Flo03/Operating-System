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

    // Bridge into kernelland: 1) start kernel, 2) stop running user, 3) wait for completion
    private static void startTheKernel() {
        if (ki == null) { ki = new Kernel(); }
        retVal = null;

        // 1) enter privileged mode
        ki.start();

        // 2) stop currently-running userland (if any)
        Scheduler s = ki.getScheduler();  // allowed accessor per assignment
        if (s.currentlyRunning != null) {
            s.currentlyRunning.stop();
        }

        // 3) block until kernel sets return value AND clears call to NONE
        while (retVal == null || currentCall != CallType.NONE) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }
    }

    // ---- Public OS "syscalls" (userland entry points) ----
    public static void switchProcess() {
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
    }

    public static void Startup(UserlandProcess init) {
        if (ki == null) { ki = new Kernel(); }
        CreateProcess(init, PriorityType.realtime);      // give init a strong start
        CreateProcess(new IdleProcess(), PriorityType.background);
    }

    public enum PriorityType { realtime, interactive, background }

    public static int CreateProcess(UserlandProcess up) {
        return CreateProcess(up, PriorityType.interactive);
    }

    // Priority is used by the new multi-queue scheduler
    public static int CreateProcess(UserlandProcess up, PriorityType priority) {
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        return (int) retVal;
    }

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

    public static void Sleep(int milliseconds) {
        parameters.clear();
        parameters.add(milliseconds);
        currentCall = CallType.Sleep;
        startTheKernel();
    }

    // --- Placeholders retained for future assignments ---
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

    // Optional accessor for tests
    static Kernel getKernel() { return ki; }
}
