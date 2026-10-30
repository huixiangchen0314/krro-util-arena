package top.kzre.krro.util.arena;

import java.util.function.Supplier;

public final class AutoGrowIntArrayArenaTemplate
        extends AutoGrowArenaTemplate<IntArrayArena, IntArenaView, IntArrayStorage> {

    public AutoGrowIntArrayArenaTemplate(
            IntArrayArena arena,
            Supplier<IntArrayStorage> storageSupplier) {
        super(arena, storageSupplier);
    }

    public static AutoGrowIntArrayArenaTemplate fixedSize(IntArrayArena arena, int size){
        return new AutoGrowIntArrayArenaTemplate(arena,
                ()->new IntArrayStorage(new int[size]));
    }

}