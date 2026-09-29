package top.kzre.krro.util.arena;

/**
 * 可增长 Arena——支持动态添加存储层。
 *
 * <p><b>显式扩容</b>：Arena 不自动扩容。调用方通过
 * {@link #addStorage} 主动添加新存储层。何时加、加多大、
 * 什么类型——全部由调用方决定。
 *
 * <p><b>为什么显式</b>：
 * <ul>
 *   <li>扩容消耗全局资源——内存 / 文件 / 虚拟地址</li>
 *   <li>策略场景相关——按需 / 提前 / 分批</li>
 *   <li>调用方知道业务——Arena 不知道</li>
 * </ul>
 *
 * <p><b>典型用法</b>：
 * <pre>
 *   AllocateResult&lt;V&gt; r = arena.allocate(storage, size);
 *   if (r.isFailure()) {
 *       arena.addStorage(newStorage(...));   // 主动加存储
 *       r = arena.allocate(newStorage, size); // 用新存储
 *   }
 * </pre>
 *
 * <p><b>线程契约</b>：非线程安全。
 */
public interface GrowableArena<V extends ArenaView<V>> extends Arena<V> {

    /**
     * 添加一个存储层。
     *
     * <p>新存储层立即参与分配——{@link Arena#getStorages} 返回它，
     * Template 的路由会考虑它。
     *
     * <p>幂等？——不。同一 storage 添加两次将导致重复条目——
     * 调用方保证不重复。
     *
     * @param storage 新存储层——不得为 null
     */
    void addStorage(Storage storage);
}