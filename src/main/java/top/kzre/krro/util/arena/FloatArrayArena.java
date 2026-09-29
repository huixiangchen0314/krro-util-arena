package top.kzre.krro.util.arena;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * 单存储层 Float Arena——底层为 float 数组。
 *
 * <p>与 {@link AbstractArena} 的关系：本类限定 {@code S = FloatArrayStorage}——
 * 视图构造时直接使用具体存储类型——无需运行时 cast。
 *
 * <p>视图是内部类——通过 {@code SingleFloatArrayArena.this} 回调
 * {@link AbstractArena#destroyView}——无需持有 arena 引用字段。
 */
public final class FloatArrayArena
        extends AbstractArena<FloatArenaView, FloatArrayStorage> {

    public FloatArrayArena(List<FloatArrayStorage> storages) {
        super(storages);
    }

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
        private final long              offset;
        private final int               byteSize;
        private final int               count;

        DefaultFloatArenaView(FloatArrayStorage storage, long offset, int byteSize) {
            this.storage  = storage;
            this.offset   = offset;
            this.byteSize = byteSize;
            this.count    = byteSize / Float.BYTES;
        }

        // ═══════════════════════════════════════════
        // 引用计数归零——归还段
        // ═══════════════════════════════════════════

        @Override
        protected void onRelease() {
            // 内部类——直接访问外层实例
            FloatArrayArena.this.destroyView(storage, offset, byteSize);
        }

        // ═══════════════════════════════════════════
        // 元数据
        // ═══════════════════════════════════════════

        @Override public long offset() { return offset; }
        @Override public long count()  { return count; }

        // ═══════════════════════════════════════════
        // 数据访问
        // ═══════════════════════════════════════════

        @Override
        public FloatBuffer floatBuffer() {
            return storage.floatSlice(offset, count);
        }

        @Override
        public float[] getFloats() {
            return storage.getData();
        }

        // ═══════════════════════════════════════════
        // 拷贝
        // ═══════════════════════════════════════════

        @Override
        public AllocateResult<FloatArenaView> copy() {
            // 1. 在同一个 storage 分配新段
            AllocateResult<FloatArenaView> result =
                    FloatArrayArena.this.allocate(storage, byteSize);
            if (result.isFailure()) return result;

            // 2. 拷贝字节
            FloatArenaView newView = result.getView();
            storage.copy(offset, newView.offset(), byteSize);

            return result;
        }
    }
}