package top.kzre.krro.util.arena;

import java.util.List;

public abstract class AbstractArenaTemplate<
        T extends Arena<V>,
        V extends ArenaView<V> > {
    private final T arena;

    public AbstractArenaTemplate(T arena) {
        this.arena = arena;
    }
    protected T getArena(){
        return arena;
    }

    /**
     * 算法核心入口。
     */
    public AllocateResult<V> allocate(int byteSize) {
        for (Storage storage : priorityStorage(arena.getStorages())) {
            AllocateResult<V> result = arena.allocate(storage, byteSize);
            if (result.isSuccess()) {
                return result;
            }
        }
        return AllocateResult.unknownFailed();
    }

    /**
     * 算法——按优先级排序 storages。
     */
    protected abstract List<Storage> priorityStorage(List<Storage> storages);
}
