package top.kzre.krro.util.arena;

/**
 * Arena 的一段视图切片。
 *
 * <p><b>身份</b>：view 的 offset + count 决定其覆盖的物理段。
 *
 * <p><b>引用计数</b>：acquire / release——归零时段归还 Arena。
 *
 * <p><b>方向性扩展</b>：{@link #tryExpandAfter} ——
 * 尝试在物理相邻位置分配空间——与本段构成更大的连续段。
 * 成功时原地扩展——identity 不变——旧数据不复制。
 */
public interface ArenaView<T extends ArenaView<T>> {

    // ═══════════════════════════════════════════════
    // 引用计数
    // ═══════════════════════════════════════════════

    /** 引用 +1。返回自增后的值。 */
    int acquire();

    /** 引用 -1。归零时段归还 Arena。返回自减后的值。 */
    int release();

    /** 当前引用计数。 */
    int refCount();

    // ═══════════════════════════════════════════════
    // 复制
    // ═══════════════════════════════════════════════

    /**
     * 复制——在 Arena 任意位置分配新段——拷贝数据。
     *
     * <p>不要求独占。新视图 refCount = 1。本视图不变。
     */
    AllocateResult<T> copy();

    // ═══════════════════════════════════════════════
    // 方向性扩展——原地
    // ═══════════════════════════════════════════════

    /**
     * 尝试在本段之后分配——与本段构成更大的连续段。
     *
     * <p><b>成功条件</b>：
     * <ul>
     *   <li>本视图独占（refCount == 1）</li>
     *   <li>本段之后的物理空间足够——未被其他段占用</li>
     * </ul>
     *
     * <p><b>成功时——原地扩展</b>：
     * <ul>
     *   <li>本视图的 byteSize / count 增加 extraBytes</li>
     *   <li>offset 不变——旧数据的块内偏移不变</li>
     *   <li>identity 不变——refCount 保持</li>
     *   <li>旧数据不复制</li>
     * </ul>
     *
     * <p><b>失败时</b>：本视图完全不变。
     *
     * @param extraBytes 额外扩展字节数——须 &gt; 0
     * @return true——扩展成功；false——不独占 / 空间不足
     */
    boolean tryExpandAfter(long extraBytes);

}