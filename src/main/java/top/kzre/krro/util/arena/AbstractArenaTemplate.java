package top.kzre.krro.util.arena;

import java.util.ArrayList;
import java.util.Comparator;
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
    protected List<Storage> priorityStorage(List<Storage> storages){
        if (storages.size() < 2) {
            return storages;
        }

        // 拷贝——不修改调用方传入的列表
        List<Storage> sorted = new ArrayList<>(storages);

        // 按容量降序——stable——同容量保持原顺序
        sorted.sort(Comparator.comparingLong(Storage::capacity).reversed());

        return sorted;
    }
}
