package top.kzre.krro.util.arena;

import top.kzre.krro.util.arena.FreeList;

import java.util.*;

/**
 * 抽象 Arena——提供空闲列表管理。
 *
 * <p>每个 Storage 一个 {@link FreeList}——初始化时把整个容量
 * 作为一段空闲放入。分配时从 FreeList 取——无单独的 bump。
 *
 * <p><b>可增长</b>：{@link #doAddStorage} 支持动态添加存储层——
 * 子类通过 {@link GrowableArena#addStorage} 暴露。新存储层
 * 立即参与分配。
 *
 * <p>子类负责构造具体视图。
 */
public abstract class AbstractArena<V extends ArenaView<V>, S extends Storage>
        implements Arena<V> {

    private final List<S>                storages;
    private final Map<Storage, FreeList> freeLists;

    protected AbstractArena(List<S> storages) {
        if (storages == null) {
            throw new IllegalArgumentException("storages cannot be null");
        }
        this.storages  = new ArrayList<>(storages);
        this.freeLists = new IdentityHashMap<>();
        for (S s : storages) {
            initFreeList(s);
        }
    }

    /** 初始化 storage 的 FreeList——整段空闲。 */
    private void initFreeList(Storage s) {
        FreeList fl = new FreeList();
        fl.free(0L, s.capacity());
        freeLists.put(s, fl);
    }

    /**
     * 添加存储层——内部实现——子类通过 {@code addStorage} 暴露。
     *
     * <p>类型转换 {@code (S)} 由子类保证——子类在
     * {@code addStorage} 中先做类型校验。
     *
     * <p>新存储层立即参与分配——{@link #getStorages} 返回它。
     */
    @SuppressWarnings("unchecked")
    protected final void doAddStorage(Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage must not be null");
        }
        if (freeLists.containsKey(storage)) {
            throw new IllegalArgumentException("storage already added");
        }
        storages.add((S) storage);
        initFreeList(storage);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Storage> getStorages() {
        return (List<Storage>) (List<?>) Collections.unmodifiableList(storages);
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

    @Override
    public long freeSize() {
        long total = 0;
        for (FreeList fl : freeLists.values()) total += fl.totalSize();
        return total;
    }

    @Override
    public long used() {
        long cap = 0;
        for (S s : storages) cap += s.capacity();
        return cap - freeSize();
    }

    /** 子类构造具体视图——收 S 类型。 */
    protected abstract AllocateResult<V> createView(S storage, long offset, int byteSize);

    /** 归还段——视图释放时调用。 */
    protected void destroyView(Storage storage, long offset, int byteSize) {
        FreeList fl = freeLists.get(storage);
        if (fl != null) fl.free(offset, byteSize);
    }

    // ═══════════════════════════════════════════════
    // 新增——方向性扩展
    // ═══════════════════════════════════════════════

    /**
     * 预留 [offset + byteSize, offset + byteSize + extraBytes) 的空间。
     *
     * <p><b>前提</b>：调用方保证独占。
     *
     * <p><b>成功</b>：从 FreeList 切出 extraBytes——返回 true。
     *
     * <p><b>失败</b>：空间不足 / 不空闲——返回 false——FreeList 不变。
     */
    protected boolean tryReserveAfter(
            Storage storage, long offset, int byteSize, int extraBytes) {
        if (extraBytes <= 0) return false;
        FreeList fl = freeLists.get(storage);
        if (fl == null) return false;
        return fl.tryAllocAt(offset + byteSize, extraBytes);
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