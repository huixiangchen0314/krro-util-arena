package top.kzre.krro.util.arena;

/**
 * Arena 的一段视图切片
 */
public interface ArenaView<T extends ArenaView<T>> {
    int acquire();
    int release();
    int refCount();
    AllocateResult<T> copy();
}
