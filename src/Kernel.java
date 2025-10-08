public class Kernel extends Process implements Device {
    private final Scheduler scheduler;
    private final VFS vfs = new VFS();

    public Kernel() {
        super();
        this.scheduler = new Scheduler(this); // pass self so scheduler can close fds on exit
    }

    public Scheduler getScheduler() { return scheduler; }

    @Override
    public void main() {
        while (true) {
            switch (OS.currentCall) {
                // ----- process APIs -----
                case CreateProcess -> {
                    UserlandProcess up = (UserlandProcess) OS.parameters.get(0);
                    OS.PriorityType p  = (OS.PriorityType) OS.parameters.get(1);
                    int pid = scheduler.CreateProcess(up, p);
                    OS.retVal = pid;
                }
                case SwitchProcess -> {
                    scheduler.SwitchProcess();
                    OS.retVal = Boolean.TRUE;
                }
                case Sleep -> {
                    int ms = (Integer) OS.parameters.get(0);
                    scheduler.Sleep(ms);
                    OS.retVal = Boolean.TRUE;
                }
                case GetPID -> {
                    OS.retVal = scheduler.GetPid();
                }
                case Exit -> {
                    scheduler.ExitCurrent();
                    OS.retVal = Boolean.TRUE;
                }

                // ----- device APIs -----
                case Open -> {
                    String s = (String) OS.parameters.get(0);
                    PCB cur = scheduler.currentlyRunning;
                    if (cur == null) { OS.retVal = -1; break; }
                    int userFd = cur.allocUserFd();
                    if (userFd < 0) { OS.retVal = -1; break; }
                    int vfsId = vfs.Open(s);
                    if (vfsId < 0) { OS.retVal = -1; break; }
                    cur.setUserFd(userFd, vfsId);
                    OS.retVal = userFd;
                }
                case Close -> {
                    int userFd = (Integer) OS.parameters.get(0);
                    PCB cur = scheduler.currentlyRunning;
                    if (cur != null) {
                        int vfsId = cur.getVfsIdFor(userFd);
                        if (vfsId >= 0) {
                            vfs.Close(vfsId);
                            cur.clearUserFd(userFd);
                        }
                    }
                    OS.retVal = Boolean.TRUE;
                }
                case Read -> {
                    int userFd = (Integer) OS.parameters.get(0);
                    int size   = (Integer) OS.parameters.get(1);
                    PCB cur = scheduler.currentlyRunning;
                    byte[] out = new byte[0];
                    if (cur != null) {
                        int vfsId = cur.getVfsIdFor(userFd);
                        if (vfsId >= 0) out = vfs.Read(vfsId, size);
                    }
                    OS.retVal = out;
                }
                case Seek -> {
                    int userFd = (Integer) OS.parameters.get(0);
                    int to     = (Integer) OS.parameters.get(1);
                    PCB cur = scheduler.currentlyRunning;
                    if (cur != null) {
                        int vfsId = cur.getVfsIdFor(userFd);
                        if (vfsId >= 0) vfs.Seek(vfsId, to);
                    }
                    OS.retVal = Boolean.TRUE;
                }
                case Write -> {
                    int userFd   = (Integer) OS.parameters.get(0);
                    byte[] data  = (byte[]) OS.parameters.get(1);
                    PCB cur = scheduler.currentlyRunning;
                    int wrote = 0;
                    if (cur != null) {
                        int vfsId = cur.getVfsIdFor(userFd);
                        if (vfsId >= 0) wrote = vfs.Write(vfsId, data);
                    }
                    OS.retVal = wrote;
                }

                case NONE, SendMessage, GetMapping, AllocateMemory, FreeMemory,
                     GetPIDByName, WaitForMessage -> {
                    if (OS.retVal == null) OS.retVal = Boolean.TRUE;
                }
            }

            // mark handled
            OS.currentCall = OS.CallType.NONE;

            // dispatch next process if any, then yield kernel
            PCB next = scheduler.currentlyRunning;
            if (next != null) next.start();
            this.stop();
        }
    }

    // ----- Device impl (not called directly from userland; used internally/for symmetry) -----
    @Override public int Open(String s) { return vfs.Open(s); }
    @Override public void Close(int id) { vfs.Close(id); }
    @Override public byte[] Read(int id, int size) { return vfs.Read(id, size); }
    @Override public void Seek(int id, int to) { vfs.Seek(id, to); }
    @Override public int Write(int id, byte[] data) { return vfs.Write(id, data); }

    // Called by Scheduler when a process is dropped/exits
    void closeAllDevicesFor(PCB pcb) {
        int[] table = pcb.getFdTable();
        for (int i = 0; i < table.length; i++) {
            int vfsId = table[i];
            if (vfsId >= 0) {
                try { vfs.Close(vfsId); } finally { pcb.clearUserFd(i); }
            }
        }
    }
}
