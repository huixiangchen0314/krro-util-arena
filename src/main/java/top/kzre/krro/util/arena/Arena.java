package top.kzre.krro.util.arena;

import java.util.List;

public interface Arena<V extends ArenaView<V> > extends AutoCloseable {

    /** 所有存储层。 */
    List<Storage> getStorages();

    /**
     * 尝试在 storage 分配指定大小的空间
     * */
    AllocateResult<V> allocate(Storage storage, int byteSize);

    /**
     * 当前空闲字节数——所有 storage 的 FreeList 之和。
     */
    long freeSize();

    /**
     * 当前已用字节数。
     */
    long used();

    void close();
}