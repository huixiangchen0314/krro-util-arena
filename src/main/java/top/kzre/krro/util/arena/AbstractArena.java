package top.kzre.krro.util.arena;

import top.kzre.krro.util.arena.internal.FreeList;

import java.util.*;

/**
 * 抽象 Arena——提供空闲列表管理。
 *
 * <p>每个 Storage 一个 {@link FreeList}——初始化时把整个容量
 * 作为一段空闲放入。分配时从 FreeList 取——无单独的 bump。
 *
 * <p>子类负责构造具体视图。
 */
public abstract class AbstractArena<V extends ArenaView<V>, S extends Storage>
        implements Arena<V> {

    private final List<S> storages;
    private final Map<Storage, FreeList> freeLists;

    protected AbstractArena(List<S> storages) {
        if (storages == null) {
            throw new IllegalArgumentException("storages cannot be null");
        }
        if (storages.isEmpty()) {
            throw new IllegalArgumentException("storages cannot be empty");
        }
        this.storages = Collections.unmodifiableList(new ArrayList<>(storages));
        this.freeLists = new HashMap<>();
        for (Storage s : storages) {
            FreeList fl = new FreeList();
            fl.free(0L, s.capacity());
            freeLists.put(s, fl);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Storage> getStorages() {
        return (List<Storage>) storages;
    }

    @Override
    public AllocateResult<V> allocate(Storage storage, int byteSize) {
        FreeList fl = freeLists.get(storage);
        if (fl == null) return AllocateResult.unknownFailed();

        long offset = fl.alloc(byteSize);
        if (offset < 0) return AllocateResult.unknownFailed();

        @SuppressWarnings("unchecked")
        S s = (S) storage;
        return createView(s, offset, byteSize);
    }

    /** 子类构造具体视图——收 S 类型。 */
    protected abstract AllocateResult<V> createView(S storage, long offset, int byteSize);

    /**
     * 归还段——视图释放时调用。
     */
    protected void destroyView(Storage storage, long offset, int byteSize) {
        FreeList fl = freeLists.get(storage);
        if (fl != null) fl.free(offset, byteSize);
    }


    @Override
    public void close() {
        Exception first = null;
        for (S s : storages) {
            try {
                s.close();
            } catch (Exception e) {
                if (first == null) first = e;
            }
        }
        if (first != null) {
            throw new RuntimeException("close failed", first);
        }
    }
}