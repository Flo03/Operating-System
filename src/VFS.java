/**
 * VFS: maps a VFS id to (Device, innerId) and routes all calls.
 * Naming: first token chooses device, remainder is the device's Open() arg.
 *   "random 100" -> RandomDevice with seed 100
 *   "file data.dat" -> FakeFileSystem for data.dat
 */
public class VFS implements Device {
    private final Device[] dev = new Device[10];
    private final int[]    inner = new int[10];

    private final RandomDevice random = new RandomDevice();
    private final FakeFileSystem ffs  = new FakeFileSystem();

    public VFS() {
        for (int i = 0; i < inner.length; i++) inner[i] = -1;
    }

    private int allocSlot() {
        for (int i = 0; i < dev.length; i++) if (dev[i] == null) return i;
        return -1;
    }

    private boolean valid(int vfsId) {
        return vfsId >= 0 && vfsId < dev.length && dev[vfsId] != null && inner[vfsId] >= 0;
    }

    private Device pickDevice(String firstToken) {
        if (firstToken == null) return null;
        String t = firstToken.toLowerCase();
        return switch (t) {
            case "random" -> random;
            case "file"   -> ffs;
            default       -> null;
        };
    }

    @Override
    public int Open(String s) {
        if (s == null || s.isBlank()) return -1;
        String trimmed = s.trim();
        String[] parts = trimmed.split("\\s+", 2);
        String devName = parts[0];
        String arg = (parts.length > 1) ? parts[1] : "";

        Device target = pickDevice(devName);
        if (target == null) return -1;

        int innerId = target.Open(arg);
        if (innerId < 0) return -1;

        int vfsSlot = allocSlot();
        if (vfsSlot < 0) {
            // no room; close inner and fail
            target.Close(innerId);
            return -1;
        }

        dev[vfsSlot] = target;
        inner[vfsSlot] = innerId;
        return vfsSlot;
    }

    @Override
    public void Close(int vfsId) {
        if (!valid(vfsId)) return;
        try {
            dev[vfsId].Close(inner[vfsId]);
        } finally {
            dev[vfsId] = null;
            inner[vfsId] = -1;
        }
    }

    @Override
    public byte[] Read(int vfsId, int size) {
        if (!valid(vfsId)) return new byte[0];
        return dev[vfsId].Read(inner[vfsId], size);
    }

    @Override
    public void Seek(int vfsId, int to) {
        if (!valid(vfsId)) return;
        dev[vfsId].Seek(inner[vfsId], to);
    }

    @Override
    public int Write(int vfsId, byte[] data) {
        if (!valid(vfsId)) return 0;
        return dev[vfsId].Write(inner[vfsId], data);
    }
}
