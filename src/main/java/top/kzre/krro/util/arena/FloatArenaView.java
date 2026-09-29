package top.kzre.krro.util.arena;

import java.nio.FloatBuffer;

/**
 * Float 视图——Arena 中一段 float 元素的逻辑句柄。
 *
 * <p><b>身份</b>：视图对象自身即身份——稳定。引用计数归零前——
 * 数据始终有效。视图不被序列化——跨会话的持久化由项目文件承担。
 *
 * <p><b>数据访问</b>：两条路径——
 * <ul>
 *   <li>{@link #floatBuffer()}——零拷贝切片——任何存储层（堆内 /
 *       堆外 / mapped）都直接支持。适合批量读写、GPU 上传、
 *       传递给底层库。</li>
 *   <li>{@link #getFloats()}——请求一份堆内的 {@code float[]}——
 *       数据不在堆内时先提升。返回的数组可能直接指向存储层
 *       （堆内情况）——调用方不得释放、不得改变长度。</li>
 * </ul>
 *
 * <p><b>提升语义</b>：{@code getFloats()} 相当于 OS 的换页——
 * 把数据从堆外 / mapped 移到堆内。一般总能成功，除非堆已满且
 * 无页可换出——此时抛 {@link ArenaAllocationException}。
 *
 * <p><b>引用计数</b>：{@link #acquire()} / {@link #release()} 管理
 * 生命周期。归零时触发段归还——调用方不应再访问视图。
 *
 * <p><b>线程契约</b>：引用计数原子——可并发。其他方法非线程安全——
 * 由调用方保证单线程访问。
 */
public interface FloatArenaView extends ArenaView<FloatArenaView> {

    /**
     * 获取本视图对应的堆内 {@code float[]}。
     *
     * <p>数据不在堆内时——先提升到堆内——再返回。
     *
     * <p>返回的数组长度等于 {@link #count()}——索引 {@code 0}
     * 对应 {@link #offset()} 起始的 float。
     *
     * <p>调用方不得持有该数组跨越 {@link #release()}——不得修改
     * 其长度。内容可写——写入直接反映到存储层。
     *
     * @return 堆内 float 数组
     * @throws ArenaAllocationException 提升失败——堆满且无法换出
     */
    float[] getFloats();

    /**
     * 本视图在存储层中的起始字节偏移。
     *
     * <p>存储层内的物理位置——不参与业务逻辑。同一视图的
     * offset 在 COW / pack 后可能改变——视图对象不变。
     */
    long offset();

    /**
     * 本视图包含的 float 元素数量。
     *
     * <p>等于 {@link #floatBuffer()} 返回缓冲区的容量、
     * {@link #getFloats()} 返回数组的长度。
     */
    long count();

    /**
     * 获取本视图的零拷贝切片——按 float 解释。
     *
     * <p>无论数据在堆内 / 堆外 / mapped——都返回可读写的
     * {@link FloatBuffer}。切片的 position 为 0、limit / capacity
     * 为 {@link #count()}。
     *
     * <p>切片共享底层内存——修改直接反映到存储层。切片本身
     * 生命周期独立于视图——但底层内存的有效性由视图引用计数保证。
     */
    FloatBuffer floatBuffer();
}