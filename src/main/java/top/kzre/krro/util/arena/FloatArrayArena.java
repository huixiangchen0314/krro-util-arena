package top.kzre.krro.util.arena;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * Float 数组 Arena——单存储类型，支持动态扩容。
 *
 * <p><b>类型约束</b>：本 Arena 只接受 {@link FloatArrayStorage}——
 * {@link #addStorage} 做运行时校验，拒绝其他类型。
 *
 * <p><b>视图</b>：内部类——通过 {@code FloatArrayArena.this}
 * 回调 {@link AbstractArena#destroyView}——无需持有 arena 引用字段。
 */
public final class FloatArrayArena
        extends AbstractArena<FloatArenaView, FloatArrayStorage>
        implements GrowableArena<FloatArenaView> {

    public FloatArrayArena(List<FloatArrayStorage> storages) {
        super(storages);
    }

    // ═══════════════════════════════════════════════
    // 扩容——类型校验
    // ═══════════════════════════════════════════════

    /**
     * 添加存储层——必须是 {@link FloatArrayStorage}。
     *
     * @throws IllegalArgumentException 类型不符
     */
    @Override
    public void addStorage(Storage storage) {
        if (!(storage instanceof FloatArrayStorage)) {
            throw new IllegalArgumentException(
                    "FloatArrayArena only accepts FloatArrayStorage, got: "
                            + (storage == null ? "null" : storage.getClass().getName()));
        }
        doAddStorage(storage);
    }

    // ═══════════════════════════════════════════════
    // 视图工厂
    // ═══════════════════════════════════════════════

    @Override
    protected AllocateResult<FloatArenaView> createView(
            FloatArrayStorage storage, long offset, int byteSize) {
        return AllocateResult.success(
                new DefaultFloatArenaView(storage, offset, byteSize));
    }

    // ═══════════════════════════════════════════════
    // 视图——内部类
    // ═══════════════════════════════════════════════

    private final class DefaultFloatArenaView
            extends AbstractArenaView<FloatArenaView>
            implements FloatArenaView {

        private final FloatArrayStorage storage;

        // 非 final——原地扩展时改
        private final long offset;
        private int  byteSize;
        private int  count;

        DefaultFloatArenaView(FloatArrayStorage storage, long offset, int byteSize) {
            this.storage  = storage;
            this.offset   = offset;
            this.byteSize = byteSize;
            this.count    = byteSize / Float.BYTES;
        }

        @Override
        protected void onRelease() {
            FloatArrayArena.this.destroyView(storage, offset, byteSize);
        }

        @Override public long offset() { return offset; }
        @Override public long count()  { return count; }

        @Override
        public FloatBuffer floatBuffer() {
            return storage.floatSlice(offset, count);
        }

        @Override
        public float[] getFloats() {
            return storage.getData();
        }

        @Override
        public AllocateResult<FloatArenaView> copy() {
            AllocateResult<FloatArenaView> result =
                    FloatArrayArena.this.allocate(storage, byteSize);
            if (result.isFailure()) return result;

            FloatArenaView newView = result.getView();
            storage.copy(offset, newView.offset(), byteSize);
            return result;
        }

        // ═══════════════════════════════════════════════
        // 方向性扩展——原地
        // ═══════════════════════════════════════════════

        @Override
        public boolean tryExpandAfter(long extraBytes) {
            if (extraBytes <= 0 || extraBytes > Integer.MAX_VALUE) return false;
            if (refCount() != 1) return false;

            int extra = (int) extraBytes;

            if (!FloatArrayArena.this.tryReserveAfter(
                    storage, offset, byteSize, extra)) {
                return false;
            }

            this.byteSize += extra;
            this.count    += extra / Float.BYTES;
            return true;
        }

    }
}