import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Fake filesystem device backed by RandomAccessFile.
 * Open("filename") -> device-local id (0..9) and opens the file "rw".
 */
public class FakeFileSystem implements Device {
    private final RandomAccessFile[] files = new RandomAccessFile[10];

    private int allocSlot() {
        for (int i = 0; i < files.length; i++) if (files[i] == null) return i;
        return -1;
    }

    private boolean valid(int id) {
        return id >= 0 && id < files.length && files[id] != null;
    }

    @Override
    public int Open(String s) {
        try {
            if (s == null || s.isBlank()) throw new IllegalArgumentException("filename required");
            int idx = allocSlot();
            if (idx < 0) return -1;
            files[idx] = new RandomAccessFile(s.trim(), "rw");
            return idx;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void Close(int id) {
        if (!valid(id)) return;
        try { files[id].close(); } catch (IOException ignored) {}
        files[id] = null;
    }

    @Override
    public byte[] Read(int id, int size) {
        if (!valid(id) || size <= 0) return new byte[0];
        byte[] buf = new byte[size];
        try {
            int total = 0;
            while (total < size) {
                int r = files[id].read(buf, total, size - total);
                if (r < 0) break; // EOF
                total += r;
            }
            if (total == size) return buf;
            byte[] trimmed = new byte[total];
            System.arraycopy(buf, 0, trimmed, 0, total);
            return trimmed;
        } catch (IOException e) {
            return new byte[0];
        }
    }

    @Override
    public void Seek(int id, int to) {
        if (!valid(id)) return;
        try { files[id].seek(Math.max(0L, to)); } catch (IOException ignored) {}
    }

    @Override
    public int Write(int id, byte[] data) {
        if (!valid(id) || data == null || data.length == 0) return 0;
        try {
            files[id].write(data);
            return data.length;
        } catch (IOException e) {
            return 0;
        }
    }
}
