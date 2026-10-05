package top.kzre.krro.util.arena;

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
        extends ArenaTemplate<FloatArrayArena, FloatArenaView> {

    public FloatArrayArenaTemplate(FloatArrayArena arena) {
        super(arena);
    }

}