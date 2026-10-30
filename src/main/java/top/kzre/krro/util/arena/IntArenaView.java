package top.kzre.krro.util.arena;

import java.nio.IntBuffer;

public interface IntArenaView extends ArenaView<IntArenaView> {
    int[] getInts();
    long offset();
    long count();

    /**
     * int 元素偏移——{@link #offset()} 除以 {@code Integer.BYTES}。
     *
     * <p>用于按元素索引访问底层 {@code int[]}：
     * <pre>
     *   int[] storage = view.getInts();          // 整个 arena 的 int[]
     *   int base = (int) view.intOffset();       // 当前视图的起始元素
     *   int v = storage[base + i];
     * </pre>
     *
     * <p>{@code offset()} 是字节偏移，直接加到 {@code int[]} 索引上
     * 会读到错误位置——必须先用本方法转成元素偏移。
     */
    default long intOffset() {
        return offset() / Integer.BYTES;
    }

    IntBuffer intBuffer();
}
