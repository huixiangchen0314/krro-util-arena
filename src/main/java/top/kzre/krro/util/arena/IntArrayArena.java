package top.kzre.krro.util.arena;

import java.nio.IntBuffer;
import java.util.List;

public final class IntArrayArena
        extends AbstractArena<IntArenaView, IntArrayStorage> {

    public IntArrayArena(List<IntArrayStorage> storages) {
        super(storages);
    }

    @Override
    protected AllocateResult<IntArenaView> createView(
            IntArrayStorage storage, long offset, int byteSize) {
        return AllocateResult.success(
                new DefaultIntArenaView(storage, offset, byteSize));
    }

    private final class DefaultIntArenaView
            extends AbstractArenaView<IntArenaView>
            implements IntArenaView {

        private final IntArrayStorage storage;
        private final long            offset;
        private final int             byteSize;
        private final int             count;

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
            IntBuffer ib = storage.intSlice(offset, count);
            int[] result = new int[count];
            ib.get(result);
            return result;
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
    }
}