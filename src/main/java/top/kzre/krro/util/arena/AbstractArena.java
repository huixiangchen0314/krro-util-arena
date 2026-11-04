package top.kzre.krro.util.arena;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 抽象 Arena——提供空闲列表管理。
 *
 * <p><b>线程安全</b>：所有结构操作（分配、释放、扩展、增删 storage、关闭）
 * 由同一把 {@link ReentrantLock} 保护，任意线程调用都安全。
 * 数据访问（{@code floatBuffer()} / {@code getFloats()} / {@code storage.copy()}）
 * 不经过本类，由视图引用计数和调用方契约保证——不同视图指向不同区域可并发，
 * 同一视图的数据读写不应跨线程并发。
 *
 */
public abstract class AbstractArena<V extends ArenaView<V>, S extends Storage>
        implements Arena<V> {

    private final List<S> storages;
    private final Map<Storage, FreeList> freeLists;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean closed = false;

    protected AbstractArena(List<S> storages) {
        if (storages == null) {
            throw new IllegalArgumentException("storages cannot be null");
        }
        this.storages = new ArrayList<>(storages);
        this.freeLists = new IdentityHashMap<>();
        for (S s : storages) {
            initFreeList(s);
        }
    }

    private void initFreeList(Storage s) {
        FreeList fl = new FreeList();
        fl.free(0L, s.capacity());
        freeLists.put(s, fl);
    }

    @SuppressWarnings("unchecked")
    protected final void doAddStorage(Storage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage must not be null");
        }
        lock.lock();
        try {
            if (closed) {
                throw new IllegalStateException("arena is closed");
            }
            if (freeLists.containsKey(storage)) {
                throw new IllegalArgumentException("storage already added");
            }
            storages.add((S) storage);
            initFreeList(storage);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Storage> getStorages() {
        lock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(storages));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public AllocateResult<V> allocate(Storage storage, long byteSize) {
        long offset;
        FreeList fl;
        lock.lock();
        try {
            if (closed) return AllocateResult.unknownFailed();
            fl = freeLists.get(storage);
            if (fl == null) return AllocateResult.unknownFailed();
            offset = fl.alloc(byteSize);
            if (offset < 0) return AllocateResult.unknownFailed();
        } finally {
            lock.unlock();
        }

        // createView 在锁外——子类可能做较重的工作，避免阻塞其他分配者
        @SuppressWarnings("unchecked")
        S s = (S) storage;
        AllocateResult<V> result;
        try {
            result = createView(s, offset, byteSize);
        } catch (RuntimeException e) {
            rollbackAlloc(fl, offset, byteSize);
            throw e;
        }
        if (result.isFailure()) {
            rollbackAlloc(fl, offset, byteSize);
        }
        return result;
    }

    private void rollbackAlloc(FreeList fl, long offset, long byteSize) {
        lock.lock();
        try {
            fl.free(offset, byteSize);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long freeSize() {
        lock.lock();
        try {
            long total = 0;
            for (FreeList fl : freeLists.values()) total += fl.totalSize();
            return total;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long used() {
        lock.lock();
        try {
            long cap = 0;
            for (S s : storages) cap += s.capacity();
            long free = 0;
            for (FreeList fl : freeLists.values()) free += fl.totalSize();
            return cap - free;
        } finally {
            lock.unlock();
        }
    }

    protected abstract AllocateResult<V> createView(S storage, long offset, long byteSize);

    /**
     * 归还段——视图释放时调用。任意线程安全。
     */
    protected void destroyView(Storage storage, long offset, long byteSize) {
        lock.lock();
        try {
            FreeList fl = freeLists.get(storage);
            if (fl != null) fl.free(offset, byteSize);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 预留 [offset + byteSize, offset + byteSize + extraBytes) 的空间。
     * 任意线程安全。
     */
    protected boolean tryReserveAfter(
            Storage storage, long offset, long byteSize, long extraBytes) {
        if (extraBytes <= 0) return false;
        lock.lock();
        try {
            FreeList fl = freeLists.get(storage);
            if (fl == null) return false;
            return fl.tryAllocAt(offset + byteSize, extraBytes);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭所有存储层。幂等。关闭后 {@link #allocate} 返回失败，
     * {@link #doAddStorage} 抛异常。
     *
     * <p>storage 的 close 在锁外执行——可能耗时，不阻塞其他线程读
     * freeSize/used。但 close 期间 allocate 已被 closed 标志挡住。
     */
    @Override
    public void close() {
        List<S> snapshot;
        lock.lock();
        try {
            if (closed) return;
            closed = true;
            snapshot = new ArrayList<>(storages);
        } finally {
            lock.unlock();
        }
        Exception first = null;
        for (S s : snapshot) {
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