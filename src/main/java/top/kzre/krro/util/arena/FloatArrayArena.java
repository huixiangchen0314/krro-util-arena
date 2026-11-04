package top.kzre.krro.util.arena;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Float 数组 Arena——单存储类型，支持动态扩容。
 *
 * <p><b>线程安全</b>：
 * <ul>
 *   <li>结构操作由 {@link AbstractArena} 的锁保护——任意线程安全。</li>
 *   <li>视图元数据 {@code byteSize} 用 {@link AtomicLong}——
 *       {@code tryExpandAfter} 的更新是原子的，不依赖「检查 refCount 后
 *       无并发 acquire」这一隐含时序假设；读者通过 {@code get()} 看到
 *       一致的最新值。</li>
 *   <li>数据访问 {@code floatBuffer()} / {@code getFloats()} 不加锁——
 *       由视图引用计数和调用方契约保证。</li>
 * </ul>
 *
 * <p><b>Session 缓存</b>：{@link FloatArenaView#array()} /
 * {@link FloatArenaView#buffer()} 返回缓存的 session 对象——无状态，
 * 避免每次访问产生垃圾。
 */
public final class FloatArrayArena
        extends AbstractArena<FloatArenaView, FloatArrayStorage>
        implements GrowableArena<FloatArenaView> {

    public FloatArrayArena(List<FloatArrayStorage> storages) {
        super(storages);
    }

    @Override
    public void addStorage(Storage storage) {
        if (!(storage instanceof FloatArrayStorage)) {
            throw new IllegalArgumentException(
                    "FloatArrayArena only accepts FloatArrayStorage, got: "
                            + (storage == null ? "null" : storage.getClass().getName()));
        }
        doAddStorage(storage);
    }

    @Override
    protected AllocateResult<FloatArenaView> createView(
            FloatArrayStorage storage, long offset, long byteSize) {
        return AllocateResult.success(
                new DefaultFloatArenaView(storage, offset, byteSize));
    }

    private final class DefaultFloatArenaView
            extends AbstractArenaView<FloatArenaView>
            implements FloatArenaView {

        private final FloatArrayStorage storage;

        /** 字节偏移——分配后不变。 */
        private final long byteOffset;
        /** 元素偏移——由 byteOffset 派生，final。 */
        private final int offset;

        /**
         * 字节大小——{@link #tryExpandAfter} 原地扩展时改。
         *
         * <p><b>为什么用 AtomicLong 而不是 volatile long</b>：
         * {@code byteSize += extraBytes} 是 read-modify-write，不是原子操作。
         * {@code refCount() == 1} 只是「检查时唯一持有者」，检查后到写之前
         * 其他线程 {@code acquire()} 让 refCount 变 2 是可能的——此时若依赖
         * 「无其他写者」的前提，代码正确性就建立在时序假设上。
         * {@link AtomicLong#addAndGet} 让更新本身原子，即使并发调用
         * {@code tryExpandAfter}（理论上被 refCount 检查阻止，但不应依赖）
         * 也不会丢失更新。
         *
         * <p>{@code count} 不单独存储，由 {@code byteSize.get() / Float.BYTES}
         * 计算——单次原子读，避免两个字段不一致。
         */
        private final AtomicLong byteSize;

        private final ArraySession  arraySession  = new DefaultArraySession();
        private final BufferSession bufferSession = new DefaultBufferSession();

        DefaultFloatArenaView(FloatArrayStorage storage, long byteOffset, long byteSize) {
            this.storage    = storage;
            this.byteOffset = byteOffset;
            this.byteSize   = new AtomicLong(byteSize);
            this.offset     = Math.toIntExact(byteOffset / Float.BYTES);
        }

        @Override
        protected void onRelease() {
            // 原子读快照——释放扩展后的完整段
            FloatArrayArena.this.destroyView(storage, byteOffset, byteSize.get());
        }

        @Override
        public ArraySession array() {
            return arraySession;
        }

        @Override
        public BufferSession buffer() {
            return bufferSession;
        }

        @Override
        public AllocateResult<FloatArenaView> copy() {
            // 原子读一次快照——copy 期间即使 tryExpandAfter 并发（理论上被
            // refCount 阻止，但不应依赖），也不会读到撕裂值。
            long size = byteSize.get();

            AllocateResult<FloatArenaView> result =
                    FloatArrayArena.this.allocate(storage, size);
            if (result.isFailure()) return result;

            FloatArenaView newView = result.getView();
            try (FloatArenaView.ArraySession session = newView.array()) {
                storage.copy(byteOffset, session.byteOffset(), size);
            } catch (Exception e) {
                // copy 失败——释放新分配的段
                newView.release();
                throw new RuntimeException(e);
            }

            return result;
        }

        @Override
        public boolean tryExpandAfter(long extraBytes) {
            if (extraBytes <= 0) return false;
            if (refCount() != 1) return false;

            // 读一次快照作为预留起点。若并发 tryExpandAfter 发生
            // （理论上被 refCount 检查阻止），两个调用读到同一个起点，
            // 第一个 tryReserveAfter 成功、第二个因区域已占用失败——
            // 只有一个 addAndGet 执行，结果正确。
            long currentSize = byteSize.get();
            if (!FloatArrayArena.this.tryReserveAfter(
                    storage, byteOffset, currentSize, extraBytes)) {
                return false;
            }

            // 原子更新——即使并发有其他写者也不会丢失更新
            byteSize.addAndGet(extraBytes);
            return true;
        }

        private final class DefaultArraySession implements ArraySession {

            @Override
            public float[] getFloats() {
                return storage.getData();
            }

            @Override
            public long byteOffset() {
                return DefaultFloatArenaView.this.byteOffset;
            }

            @Override
            public int offset() {
                return DefaultFloatArenaView.this.offset;
            }

            @Override
            public int count() {
                // 原子读一次，转元素数
                return Math.toIntExact(DefaultFloatArenaView.this.byteSize.get() / Float.BYTES);
            }

            @Override
            public void close() {
                // 无换页，NOP
            }
        }

        private final class DefaultBufferSession implements BufferSession {

            @Override
            public FloatBuffer floatBuffer() {
                // 原子读一次快照——byteOffset / count 来自同一次读
                long size = DefaultFloatArenaView.this.byteSize.get();
                return storage.floatSlice(
                        DefaultFloatArenaView.this.byteOffset,
                        Math.toIntExact(size / Float.BYTES));
            }

            @Override
            public void close() {
                // 无换页，NOP
            }
        }
    }
}