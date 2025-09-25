import java.util.ArrayList;
import java.util.List;

/**
 * OS syscall façade. Userland calls these static methods,
 * which set up parameters and trap into the Kernel.
 */
public class OS {

    private static Kernel ki; // The one and only one instance of the kernel.

    public static List<Object> parameters = new ArrayList<>();
    public static Object retVal;

    public enum CallType {
        SwitchProcess, SendMessage, Open, Close, Read, Seek, Write,
        GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory,
        FreeMemory, GetPIDByName, WaitForMessage, Exit, NONE
    }
    public static CallType currentCall = CallType.NONE;

    public enum PriorityType { realtime, interactive, background }

    /* --------------------- public syscalls --------------------- */

    /** Create a process with a given priority; returns PID. */
    public static int CreateProcess(UserlandProcess up, PriorityType p) {
        ensureKernel();
        parameters.clear();
        parameters.add(up);
        parameters.add(p);
        currentCall = CallType.CreateProcess;
        ki.start();                 // enter kernel; kernel will pick who runs next
        Object rv = retVal;         // capture return (set by kernel)
        retVal = null;
        return (rv instanceof Integer) ? (Integer) rv : -1;
    }

    /** Voluntary sleep for at least ms milliseconds. */
    public static void Sleep(int milliseconds) {
        ensureKernel();
        parameters.clear();
        parameters.add(milliseconds);
        currentCall = CallType.Sleep;
        ki.start();                 // kernel moves current to sleep queue and schedules next
        retVal = null;
    }

    /** Get PID of the currently running process. */
    public static int GetPid() {
        ensureKernel();
        parameters.clear();
        currentCall = CallType.GetPID;
        ki.start();
        Object rv = retVal;
        retVal = null;
        return (rv instanceof Integer) ? (Integer) rv : -1;
    }

    /** Terminate the current process immediately. */
    public static void Exit() {
        ensureKernel();
        parameters.clear();
        currentCall = CallType.Exit;
        ki.start();                 // kernel will unschedule current and pick another
        retVal = null;
    }

    /** Explicit cooperative yield (usually called from Process.cooperate()). */
    public static void SwitchProcess() {
        ensureKernel();
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        ki.start();
        retVal = null;
    }

    /* ---- Compatibility alias for skeletons that call OS.switchProcess() ---- */
    public static void switchProcess() {
        SwitchProcess();
    }

    /* ---------------- App startup helpers (to satisfy Main.java) ------------- */

    /** Start the OS with a specific initial process instance (e.g., new Init()). */
    public static void Startup(UserlandProcess initial) {
        ensureKernel();
        // Put the initial process in a sensible default priority (interactive).
        CreateProcess(initial, PriorityType.interactive);
        // Kick the scheduler once to start running somebody.
        SwitchProcess();
    }

    /** Start with a class literal (e.g., Init.class). Requires no-arg ctor. */
    public static void Startup(Class<? extends UserlandProcess> clazz) {
        try {
            UserlandProcess p = clazz.getDeclaredConstructor().newInstance();
            Startup(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    /* --------------------- internal helpers --------------------- */

    private static void ensureKernel() {
        if (ki == null) {
            ki = new Kernel();
        }
    }
}
