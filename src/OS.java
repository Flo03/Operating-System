import java.util.ArrayList;
import java.util.List;

public class OS {
    private static Kernel ki;

    public static List<Object> parameters = new ArrayList<>();
    public static Object retVal;
    public static CallType currentCall = CallType.NONE;

    // ----- Process/syscall helpers -----
    private static void ensureKernel() {
        if (ki == null) ki = new Kernel();
    }

    private static void startTheKernel() {
        ensureKernel();
        ki.start();
    }

    // ----- Process syscalls -----
    public static int CreateProcess(UserlandProcess up, PriorityType p) {
        parameters.clear();
        parameters.add(up);
        parameters.add(p);
        currentCall = CallType.CreateProcess;
        startTheKernel();
        Object rv = retVal; retVal = null;
        return (rv instanceof Integer) ? (Integer) rv : -1;
    }

    public static void Sleep(int milliseconds) {
        parameters.clear();
        parameters.add(milliseconds);
        currentCall = CallType.Sleep;
        startTheKernel();
        retVal = null;
    }

    public static int GetPid() {
        parameters.clear();
        currentCall = CallType.GetPID;
        startTheKernel();
        Object rv = retVal; retVal = null;
        return (rv instanceof Integer) ? (Integer) rv : -1;
    }

    public static void Exit() {
        parameters.clear();
        currentCall = CallType.Exit;
        startTheKernel();
        retVal = null;
    }

    public static void SwitchProcess() {
        parameters.clear();
        currentCall = CallType.SwitchProcess;
        startTheKernel();
        retVal = null;
    }

    public static void switchProcess() { SwitchProcess(); }

    // ----- Devices (userland entry points) -----
    public static int Open(String s) {
        parameters.clear();
        parameters.add(s);
        currentCall = CallType.Open;
        startTheKernel();
        Object rv = retVal; retVal = null;
        return (rv instanceof Integer) ? (Integer) rv : -1;
    }

    public static void Close(int id) {
        parameters.clear();
        parameters.add(id);
        currentCall = CallType.Close;
        startTheKernel();
        retVal = null;
    }

    public static byte[] Read(int id, int size) {
        parameters.clear();
        parameters.add(id);
        parameters.add(size);
        currentCall = CallType.Read;
        startTheKernel();
        Object rv = retVal; retVal = null;
        return (rv instanceof byte[]) ? (byte[]) rv : new byte[0];
    }

    public static void Seek(int id, int to) {
        parameters.clear();
        parameters.add(id);
        parameters.add(to);
        currentCall = CallType.Seek;
        startTheKernel();
        retVal = null;
    }

    public static int Write(int id, byte[] data) {
        parameters.clear();
        parameters.add(id);
        parameters.add(data);
        currentCall = CallType.Write;
        startTheKernel();
        Object rv = retVal; retVal = null;
        return (rv instanceof Integer) ? (Integer) rv : 0;
    }

    // ----- Startup -----
    public static void Startup(UserlandProcess initial) {
        ensureKernel();
        CreateProcess(initial, PriorityType.interactive);
        SwitchProcess();
    }

    public static void Startup(Class<? extends UserlandProcess> clazz) {
        try {
            UserlandProcess p = clazz.getDeclaredConstructor().newInstance();
            Startup(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + clazz.getName(), e);
        }
    }

    // ----- Enums -----
    public enum CallType {
        SwitchProcess,
        SendMessage,
        Open,
        Close,
        Read,
        Seek,
        Write,
        GetMapping,
        CreateProcess,
        Sleep,
        GetPID,
        AllocateMemory,
        FreeMemory,
        GetPIDByName,
        WaitForMessage,
        Exit,
        NONE
    }

    public enum PriorityType {
        realtime,
        interactive,
        background
    }
}
