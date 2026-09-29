package top.kzre.krro.util.arena;

import java.util.function.Supplier;

/**
 * Float Array Arena 自动扩容模板——泛型版的特化便捷别名。
 */
public final class AutoGrowFloatArrayArenaTemplate
        extends AutoGrowArenaTemplate<FloatArrayArena, FloatArenaView, FloatArrayStorage> {

    public AutoGrowFloatArrayArenaTemplate(
            FloatArrayArena arena,
            Supplier<FloatArrayStorage> storageSupplier) {
        super(arena, storageSupplier);
    }
}