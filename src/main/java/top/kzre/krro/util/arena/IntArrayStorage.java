package top.kzre.krro.util.arena;

import java.nio.IntBuffer;

public final class IntArrayStorage implements IntStorage {

    private int[] data;
    private final long capacity;

    public IntArrayStorage(int[] data) {
        this.data     = data;
        this.capacity = (long) data.length * Integer.BYTES;
    }

    public int[] getData() { return data; }

    @Override public StorageKind getKind() { return StorageKind.HEAP; }
    @Override public long capacity() { return capacity; }

    @Override
    public void copy(long srcByteOffset, long dstByteOffset, long byteSize) {
        if (data == null) throw new IllegalStateException("closed");
        int srcIdx = (int) (srcByteOffset / Integer.BYTES);
        int dstIdx = (int) (dstByteOffset / Integer.BYTES);
        int count  = (int) (byteSize      / Integer.BYTES);
        System.arraycopy(data, srcIdx, data, dstIdx, count);
    }

    @Override
    public IntBuffer intSlice(long byteOffset, int count) {
        if (data == null) throw new IllegalStateException("closed");
        int idx = (int) (byteOffset / Integer.BYTES);
        return IntBuffer.wrap(data, idx, count);
    }

    @Override
    public void close() {
        this.data = null;
    }
}