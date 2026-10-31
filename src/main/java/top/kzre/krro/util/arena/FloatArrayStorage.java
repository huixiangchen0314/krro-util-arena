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
        int srcIdx = Math.toIntExact((srcByteOffset / Float.BYTES));
        int dstIdx = Math.toIntExact((dstByteOffset / Float.BYTES));
        int count  = Math.toIntExact((byteSize / Float.BYTES));
        System.arraycopy(data, srcIdx, data, dstIdx, count);
    }

    @Override
    public FloatBuffer floatSlice(long byteOffset, long count) {
        if (data == null) throw new IllegalStateException("closed");
        int idx = Math.toIntExact((byteOffset / Float.BYTES));
        // 截断，想更多精度支持，换存储
        return FloatBuffer.wrap(data, idx, Math.toIntExact(count));
    }

    @Override
    public void close() {
        this.data = null;
    }
}