import java.util.Random;

/**
 * Random device with up to 10 independent RNG instances.
 * Open("seed") -> slot id (0..9). If seed is not numeric or missing, uses default seeding.
 * Read(id, n) -> n random bytes.
 * Seek(id, n) -> advances RNG by n bytes (discard).
 * Write(...) -> returns 0 (no-op).
 */
public class RandomDevice implements Device {
    private final Random[] slots = new Random[10];

    private int allocSlot() {
        for (int i = 0; i < slots.length; i++) if (slots[i] == null) return i;
        return -1;
    }

    private boolean valid(int id) {
        return id >= 0 && id < slots.length && slots[id] != null;
    }

    @Override
    public int Open(String s) {
        int idx = allocSlot();
        if (idx < 0) return -1;

        if (s != null && !s.isBlank()) {
            try {
                long seed = Long.parseLong(s.trim());
                slots[idx] = new Random(seed);
            } catch (NumberFormatException e) {
                slots[idx] = new Random();
            }
        } else {
            slots[idx] = new Random();
        }
        return idx;
    }

    @Override
    public void Close(int id) {
        if (valid(id)) slots[id] = null;
    }

    @Override
    public byte[] Read(int id, int size) {
        if (!valid(id) || size <= 0) return new byte[0];
        byte[] out = new byte[size];
        for (int i = 0; i < size; i++) out[i] = (byte) (slots[id].nextInt(256));
        return out;
    }

    @Override
    public void Seek(int id, int to) {
        if (!valid(id) || to <= 0) return;
        for (int i = 0; i < to; i++) slots[id].nextInt(256);
    }

    @Override
    public int Write(int id, byte[] data) {
        return 0; // no-op
    }
}
