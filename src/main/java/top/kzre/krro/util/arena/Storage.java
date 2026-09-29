package top.kzre.krro.util.arena;

/**
 * 存储实体——字节级的物理后端。
 *
 * <p><b>职责</b>：管理一段连续字节内存。提供容量查询、段内拷贝、
 * 生命周期关闭。类型化的数据访问（{@code floatSlice} /
 * {@code intSlice} / {@code byteSlice}）由子接口
 * {@link FloatStorage} / {@link IntStorage}
 * 分别承载——本接口不含任何数据解释。
 *
 * <p><b>与 Arena 的关系</b>：Arena 持有多个 Storage——每个 Storage
 * 对应一个空闲列表（在 Arena 侧）。Storage 不参与分配——它只是
 * "物理内存"的抽象。分配决策 / 空闲管理 / 段生命周期全部由
 * Arena 层负责。
 *
 * <p><b>与 ArenaView 的关系</b>：View 持有一个 Storage 引用与一段
 * offset / size——通过 Storage 的类型化切片访问数据。View 释放时
 * 通知 Arena 归还段——Storage 本身不感知 View。
 *
 * <p><b>存储种类</b>：见 {@link StorageKind}——堆内、堆外、mmap。
 * 不同种类在访问性能、内存上限、可持久化性上各有取舍。
 *
 * <p><b>线程契约</b>：非线程安全。所有方法必须在单线程中调用——
 * 通常由 Arena 保证。
 */
public interface Storage extends AutoCloseable {

    /**
     * 存储种类——堆内 / 堆外 / mapped。
     *
     * <p>用于策略判断：例如堆内的 {@code getFloats()} 可以零拷贝
     * 返回底层数组，堆外则需先提升。
     */
    StorageKind getKind();

    /**
     * 总容量——字节。
     *
     * <p>固定不变——Storage 创建后容量即确定。扩容通过 Arena
     * 添加新 Storage 实现——不是修改现有 Storage。
     */
    long capacity();

    /**
     * 存储内段拷贝——把 {@code [srcOffset, srcOffset + byteSize)}
     * 的数据移到 {@code [dstOffset, dstOffset + byteSize)}。
     *
     * <p><b>用途</b>：pack 时移动活跃段——整理碎片。同一 Storage
     * 内的移动——不需要经过 ArenaView 层。
     *
     * <p><b>重叠</b>：src 与 dst 区间可能重叠——实现需正确处理
     * （{@code System.arraycopy} 语义）。
     *
     * <p><b>越界</b>：调用方保证区间在容量内——实现可假设。若
     * 需要防御——实现自行检查并抛 {@link IndexOutOfBoundsException}。
     *
     * @param srcOffset 源起始——字节
     * @param dstOffset 目标起始——字节
     * @param byteSize  拷贝字节数
     */
    void copy(long srcOffset, long dstOffset, long byteSize);

    /**
     * 关闭存储——释放底层资源。
     *
     * <p><b>幂等</b>：多次调用不应抛异常。已关闭的 Storage 再次
     * 访问数据（切片 / 拷贝）——应抛 {@link IllegalStateException}。
     *
     * <p><b>与 View 的关系</b>：Storage 关闭时——其上所有 View 应
     * 已释放。Arena 负责保证这个顺序——Storage 不追踪 View。
     */
    @Override
    void close();
}