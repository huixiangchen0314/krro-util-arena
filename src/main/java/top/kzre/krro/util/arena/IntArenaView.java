package top.kzre.krro.util.arena;

import java.nio.IntBuffer;

/**
 * Int 视图——Arena 中一段 int 元素的逻辑句柄。
 *
 * <p><b>身份</b>：视图对象自身即身份——稳定。引用计数归零前——
 * 数据始终有效。视图不被序列化——跨会话的持久化由项目文件承担。
 *
 * <p><b>数据访问</b>：两条路径——
 * <ul>
 *   <li>{@link BufferSession#intBuffer()}——零拷贝切片——任何存储层
 *       （堆内 / 堆外 / mapped）都直接支持。适合批量读写、GPU 上传、
 *       传递给底层库。</li>
 *   <li>{@link ArraySession#getInts()}——请求一份堆内的 {@code int[]}——
 *       数据不在堆内时先提升。返回的数组可能直接指向存储层
 *       （堆内情况）——调用方不得释放、不得改变长度。</li>
 * </ul>
 *
 * <p><b>提升语义</b>：{@code getInts()} 相当于 OS 的换页——
 * 把数据从堆外 / mapped 移到堆内。一般总能成功，除非堆已满且
 * 无页可换出——此时抛 {@link ArenaAllocationException}。
 *
 * <p><b>引用计数</b>：{@link #acquire()} / {@link #release()} 管理
 * 生命周期。归零时触发段归还——调用方不应再访问视图。
 *
 * <p><b>线程契约</b>：引用计数原子——可并发。其他方法非线程安全——
 * 由调用方保证单线程访问。
 */
public interface IntArenaView extends ArenaView<IntArenaView> {

    /**
     * 开启使用会话，使用时候不会被换页，安全。
     * 如果数据是在非堆内存的话，会被自动提升为堆内存
     */
    ArraySession array();

    /**
     * 开启会话. 返回Buffer，这个抽象允许非堆抽象
     */
    BufferSession buffer();

    interface ArraySession extends AutoCloseable {

        /**
         * 获取本视图对应的堆内 {@code int[]}。
         *
         * <p>数据不在堆内时——先提升到堆内——再返回。
         *
         * <p>返回的数组长度等于 {@link #count()}——索引 {@code 0}
         * 对应 {@link #byteOffset()} 起始的 int。
         *
         * <p>调用方不得持有该数组跨越 {@link #close()}——不得修改
         * 其长度。内容可写——写入直接反映到存储层。
         *
         * @return 堆内 int 数组
         * @throws ArenaAllocationException 提升失败——堆满且无法换出
         */
        int[] getInts();

        /**
         * 本视图在存储层中的起始字节偏移。
         *
         * <p>存储层内的物理位置——不参与业务逻辑。同一视图的
         * byteOffset 在 COW / pack 后可能改变——视图对象不变。
         */
        long byteOffset();

        /**
         * int 元素偏移——{@link #byteOffset()} 除以 {@code Integer.BYTES}。
         *
         * <p>用于按元素索引访问底层 {@code int[]}：
         * <pre>
         *   int[] storage = view.array().getInts();       // 整个 arena 的 int[]
         *   int base = view.array().offset();             // 当前视图的起始元素
         *   int v = storage[base + i];
         * </pre>
         *
         * <p>{@code byteOffset()} 是字节偏移，直接加到 {@code int[]} 索引上
         * 会读到错误位置——必须用本方法转成元素偏移。
         */
        int offset();

        /**
         * 本视图包含的 int 元素数量。
         *
         * <p>等于 {@link BufferSession#intBuffer()} 返回缓冲区的容量、
         * {@link #getInts()} 返回数组的有效长度。
         */
        int count();

        /**
         * 结束会话。无换页时 NOP；有换页时可能回收临时提升。
         */
        @Override
        void close();
    }

    interface BufferSession extends AutoCloseable {

        /**
         * 获取本视图的零拷贝切片——按 int 解释。
         *
         * <p>无论数据在堆内 / 堆外 / mapped——都返回可读写的
         * {@link IntBuffer}。切片的 position 为 0、limit / capacity
         * 为 {@link ArraySession#count()}。
         *
         * <p>切片共享底层内存——修改直接反映到存储层。切片本身
         * 生命周期独立于视图——但底层内存的有效性由视图引用计数保证。
         */
        IntBuffer intBuffer();

        /**
         * 结束会话。无换页时 NOP。
         */
        @Override
        void close();
    }
}