package top.kzre.krro.util.arena;

import java.nio.FloatBuffer;

public final class FloatArrayStorage implements FloatStorage {

    private float[] data;
    private final long capacity;

    public FloatArrayStorage(float[] data) {
        this.data     = data;
        this.capacity = (long) data.length * Float.BYTES;
    }

    public float[] getData(){
        return data;
    }

    @Override
    public StorageKind getKind() { return StorageKind.HEAP; }

    @Override
    public long capacity() { return capacity; }

    @Override
    public void copy(long srcByteOffset, long dstByteOffset, long byteSize) {
        if (data == null) throw new IllegalStateException("closed");
        int srcIdx = (int) (srcByteOffset / Float.BYTES);
        int dstIdx = (int) (dstByteOffset / Float.BYTES);
        int count  = (int) (byteSize      / Float.BYTES);
        System.arraycopy(data, srcIdx, data, dstIdx, count);
    }

    @Override
    public FloatBuffer floatSlice(long byteOffset, int count) {
        if (data == null) throw new IllegalStateException("closed");
        int idx = (int) (byteOffset / Float.BYTES);
        return FloatBuffer.wrap(data, idx, count);
    }

    @Override
    public void close() {
        this.data = null;
    }
}