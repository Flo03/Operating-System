import java.util.ArrayList;
import java.util.List;

public class OS {
    private static Kernel ki; // The one and only one instance of the kernel.

    public static List<Object> parameters = new ArrayList<>();
    public static Object retVal;

    public enum CallType {
        SwitchProcess, SendMessage, Open, Close, Read, Seek, Write,
        GetMapping, CreateProcess, Sleep, GetPID, AllocateMemory,
        FreeMemory, GetPIDByName, WaitForMessage, Exit
    }
    public static CallType currentCall;

    private static void startTheKernel() {
        // Ensure single kernel instance.
        if (ki == null) {
            ki = new Kernel();
        }
        // New call in flight; clear return slot.
        retVal = null;

        // Start the kernel (enter privileged mode).
        ki.start();

        // If there is a currently running user process, pause it (simulate mode switch).
        Scheduler s = ki.getScheduler();
        if (s.currentlyRunning != null) {
            s.currentlyRunning.stop();
        }

        // Cold start case: wait until the kernel handles the call and sets retVal.
        while (retVal == null) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) { }
        }
    }

    public static void switchProcess() {
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
    }

    public static void Startup(UserlandProcess init) {
        // Instantiate kernel once and create initial processes via normal call path.
        ki = new Kernel();
        CreateProcess(init, PriorityType.interactive);
        CreateProcess(new IdleProcess(), PriorityType.background);
    }

    public enum PriorityType {realtime, interactive, background}

    public static int CreateProcess(UserlandProcess up) {
        return  CreateProcess(up,PriorityType.interactive);
    }

    // For assignment 1, you can ignore the priority. We will use that in assignment 2
    public static int CreateProcess(UserlandProcess up, PriorityType priority) {
        parameters.clear();
        parameters.add(up);
        parameters.add(priority);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        return (int) retVal;
    }

    // -------- Reserved for later assignments --------
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
    public static int AllocateMemory(int size ) { return 0; }
    public static boolean FreeMemory(int pointer, int size) { return false; }
}
