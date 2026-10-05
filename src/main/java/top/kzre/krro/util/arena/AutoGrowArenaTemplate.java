package top.kzre.krro.util.arena;

import java.util.function.Supplier;

/**
 * 自动扩容 Arena 模板——泛型版。
 *
 * <p><b>类型参数</b>：
 * <ul>
 *   <li>{@code T}——GrowableArena 的具体类型</li>
 *   <li>{@code V}——视图类型</li>
 *   <li>{@code S}——Storage 的具体类型</li>
 * </ul>
 *
 * <p><b>扩容策略</b>：分配失败时——通过 {@link #storageSupplier}
 * 取一个新 storage——加入 arena——再次尝试分配。
 *
 * <p><b>只扩一次</b>：单次 {@link #allocate} 最多触发一次扩容。
 * 如果新增的 storage 仍容不下请求——返回失败——不继续扩。避免
 * supplier 行为异常时陷入无限循环。
 *
 * <p><b>容量校验</b>：新增的 storage 必须能容下请求——否则
 * close 掉、抛 {@link ArenaAllocationException}。这是编程错误
 * （请求超过单页上限），不是正常分配失败。
 *
 * <p><b>扩容决策在调用方</b>：{@code storageSupplier} 决定新 storage
 * 的大小 / 类型。模板不猜测——不自作主张。
 *
 * <p><b>线程契约</b>：非线程安全——继承自 {@link ArenaTemplate}。
 */
public class AutoGrowArenaTemplate<
        T extends GrowableArena<V>,
        V extends ArenaView<V>,
        S extends Storage>
        extends ArenaTemplate<T, V> {

    private final Supplier<S> storageSupplier;

    /**
     * @param arena            目标 arena——非 null——且必须 Growable
     * @param storageSupplier  扩容供应器——每次调用返回一个新 storage——
     *                         返回 null 表示放弃扩容
     */
    public AutoGrowArenaTemplate(T arena, Supplier<S> storageSupplier) {
        super(arena);
        if (storageSupplier == null) {
            throw new IllegalArgumentException("storageSupplier must not be null");
        }
        this.storageSupplier = storageSupplier;
    }

    // ═══════════════════════════════════════════════
    // 扩容分配
    // ═══════════════════════════════════════════════

    @Override
    public AllocateResult<V> allocate(int byteSize) {
        // 1. 常规分配
        AllocateResult<V> result = super.allocate(byteSize);
        if (result.isSuccess()) return result;

        // 2. 失败——取新 storage
        S newStorage = storageSupplier.get();

        // 3. supplier 放弃——返回原始失败
        if (newStorage == null) return result;

        // 4. 容量校验——不够就是编程错误
        long capacity = newStorage.capacity();
        if (capacity < byteSize) {
            try {
                newStorage.close();
            } catch (Exception ignored) {
                // close 失败——以原始异常优先
            }
            throw new ArenaAllocationException(
                    "requested size " + byteSize
                            + " exceeds maximum storage capacity " + capacity
                            + " (supplier returned storage too small)");
        }

        // 5. 加入 arena
        getArena().addStorage(newStorage);

        // 6. 重试一次
        return super.allocate(byteSize);
    }
}