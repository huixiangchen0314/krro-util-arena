package top.kzre.krro.util.arena;


public interface Storage extends AutoCloseable {
    StorageKind getKind();
    long capacity();


    void copy(long srcOffset, long dstOffset, long byteSize);

    @Override void close();
}