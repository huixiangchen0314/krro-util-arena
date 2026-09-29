package top.kzre.krro.util.arena;

import java.nio.FloatBuffer;

/**
 * Float 存储。
 *
 * <p>类型化的切片是唯一入口——不管物理在哪（堆内 / 堆外 / mapped），
 * 都返回 {@link FloatBuffer}。
 */
public interface FloatStorage extends Storage {

    /**
     * Float 切片——按 float 解释。
     *
     * @param byteOffset 字节偏移
     * @param count      float 元素数
     */
    FloatBuffer floatSlice(long byteOffset, int count);
}