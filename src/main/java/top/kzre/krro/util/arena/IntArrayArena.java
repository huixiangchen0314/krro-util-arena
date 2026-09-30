package top.kzre.krro.util.arena;

import java.nio.IntBuffer;
import java.util.List;

/**
 * Int 数组 Arena——单存储类型，支持动态扩容。
 *
 * <p><b>类型约束</b>：本 Arena 只接受 {@link IntArrayStorage}——
 * {@link #addStorage} 做运行时校验，拒绝其他类型。
 *
 * <p><b>视图</b>：内部类——通过 {@code IntArrayArena.this}
 * 回调 {@link AbstractArena#destroyView}——无需持有 arena 引用字段。
 */
public final class IntArrayArena
        extends AbstractArena<IntArenaView, IntArrayStorage>
        implements GrowableArena<IntArenaView> {

    public IntArrayArena(List<IntArrayStorage> storages) {
        super(storages);
    }

    // ═══════════════════════════════════════════════
    // 扩容——类型校验
    // ═══════════════════════════════════════════════

    /**
     * 添加存储层——必须是 {@link IntArrayStorage}。
     *
     * @throws IllegalArgumentException 类型不符
     */
    @Override
    public void addStorage(Storage storage) {
        if (!(storage instanceof IntArrayStorage)) {
            throw new IllegalArgumentException(
                    "IntArrayArena only accepts IntArrayStorage, got: "
                            + (storage == null ? "null" : storage.getClass().getName()));
        }
        doAddStorage(storage);
    }

    // ═══════════════════════════════════════════════
    // 视图工厂
    // ═══════════════════════════════════════════════

    @Override
    protected AllocateResult<IntArenaView> createView(
            IntArrayStorage storage, long offset, int byteSize) {
        return AllocateResult.success(
                new DefaultIntArenaView(storage, offset, byteSize));
    }

    // ═══════════════════════════════════════════════
    // 视图——内部类
    // ═══════════════════════════════════════════════

    private final class DefaultIntArenaView
            extends AbstractArenaView<IntArenaView>
            implements IntArenaView {

        private final IntArrayStorage storage;

        private final long offset;
        private int  byteSize;
        private int  count;

        DefaultIntArenaView(IntArrayStorage storage, long offset, int byteSize) {
            this.storage  = storage;
            this.offset   = offset;
            this.byteSize = byteSize;
            this.count    = byteSize / Integer.BYTES;
        }

        @Override
        protected void onRelease() {
            IntArrayArena.this.destroyView(storage, offset, byteSize);
        }

        @Override public long offset() { return offset; }
        @Override public long count()  { return count; }

        @Override
        public IntBuffer intBuffer() {
            return storage.intSlice(offset, count);
        }

        @Override
        public int[] getInts() {
            return storage.getData();
        }

        @Override
        public AllocateResult<IntArenaView> copy() {
            AllocateResult<IntArenaView> result =
                    IntArrayArena.this.allocate(storage, byteSize);
            if (result.isFailure()) return result;

            IntArenaView newView = result.getView();
            storage.copy(offset, newView.offset(), byteSize);
            return result;
        }

        @Override
        public boolean tryExpandAfter(long extraBytes) {
            if (extraBytes <= 0 || extraBytes > Integer.MAX_VALUE) return false;
            if (refCount() != 1) return false;

            int extra = (int) extraBytes;

            if (!IntArrayArena.this.tryReserveAfter(
                    storage, offset, byteSize, extra)) {
                return false;
            }

            this.byteSize += extra;
            this.count    += extra / Integer.BYTES;
            return true;
        }


    }
}