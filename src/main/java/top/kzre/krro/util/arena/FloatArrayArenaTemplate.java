package top.kzre.krro.util.arena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Float 数组 Arena 模板——多存储层的路由策略。
 *
 * <p>按容量降序排优先级——大容量存储优先尝试。同容量保持
 * 原有顺序（stable sort）。
 *
 * <p>为什么按容量：不知道各存储的空闲空间时，容量大者更可能
 * 容纳下分配请求。调用方也可继承本类覆盖策略（如按空闲率 /
 * 按使用频率 / 按 NUMA 亲和）。
 */
public final class FloatArrayArenaTemplate
        extends AbstractArenaTemplate<FloatArrayArena, FloatArenaView> {

    public FloatArrayArenaTemplate(FloatArrayArena arena) {
        super(arena);
    }

    @Override
    protected List<Storage> priorityStorage(List<Storage> storages) {
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