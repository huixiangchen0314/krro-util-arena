package top.kzre.krro.util.arena.internal;

/**
 * 空闲段列表——按 offset 升序维护可用内存段。
 *
 * <p><b>用途</b>：Arena 分配器用它追踪所有已释放但可复用的字节区间。
 * {@link #alloc} 从中切出请求大小的段；{@link #free} 把段归还，
 * 并立即与相邻空闲段合并。
 *
 * <p><b>数据结构</b>：两个并行的 {@code long[]}——
 * {@code offsets[i]} 是第 i 个空闲段的起始偏移，
 * {@code sizes[i]} 是其字节长度。两者一一对应，{@code count} 表示有效长度。
 * 用裸数组而非对象列表，是为了避免每条记录 ~40 字节的对象开销
 * （对象头 + 两个字段 + 列表引用），并把 GC 压力降到零。
 *
 * <p><b>有序性</b>：{@code offsets} 始终按升序排列。这个不变量让
 * "合并相邻段"只需检查前后邻居——无需排序或哈希。
 *
 * <p><b>合并策略</b>：{@link #free} 在插入时立即检查左右邻居：
 * <ul>
 *   <li>左邻 {@code offsets[i-1] + sizes[i-1] == offset}——拼接</li>
 *   <li>右邻 {@code offset + size == offsets[i]}——拼接</li>
 *   <li>两侧都相邻——三者合一</li>
 * </ul>
 * 因此列表在任意时刻都不含相邻段——无需独立的 coalesce 遍历。
 *
 * <p><b>分配策略</b>：{@link #alloc} 用 first-fit——扫描到第一个
 * {@code size >= 请求} 的段即返回。未做 size 分桶，因为 Arena 场景
 * 以"少而大"的段为主（视图级的块），桶化带来的分类开销和实现复杂度
 * 得不偿失。
 *
 * <p><b>对齐</b>：本类不处理对齐——调用方负责把请求和释放的大小
 * 对齐到 Arena 的粒度（通常是 8 字节）。这样 FreeList 只关心"字节"，
 * 与元素类型、对齐规则完全解耦。
 *
 * <p><b>线程契约</b>：非线程安全。所有方法必须在单线程中调用——
 * Arena 本身也是单线程的，调用方负责隔离。
 */
public final class FreeList {

    /** 空闲段的起始偏移——升序。 */
    private long[] offsets;

    /** 空闲段的字节长度——与 {@link #offsets} 一一对应。 */
    private long[] sizes;

    /** 有效条目数——数组前 {@code count} 项是有效数据。 */
    private int    count;

    public FreeList() {
        this(16);
    }

    public FreeList(int capacity) {
        int cap = Math.max(capacity, 1);
        this.offsets = new long[cap];
        this.sizes   = new long[cap];
        this.count   = 0;
    }

    // ═══════════════════════════════════════════════
    // 分配——first-fit
    // ═══════════════════════════════════════════════

    /**
     * 分配 size 字节。
     *
     * <p>线性扫描——第一个 size >= 请求的段。
     * 找到后——若等大——移除；否则——切分——前部留新段。
     *
     * @return 分配到的 offset——失败返回 -1
     */
    public long alloc(long size) {
        for (int i = 0; i < count; i++) {
            if (sizes[i] >= size) {
                long offset = offsets[i];
                if (sizes[i] == size) {
                    removeAt(i);
                } else {
                    offsets[i] += size;
                    sizes[i]   -= size;
                }
                return offset;
            }
        }
        return -1L;
    }

    // ═══════════════════════════════════════════════
    // 释放——加入 + 合并
    // ═══════════════════════════════════════════════

    /**
     * 释放段——加入空闲列表——与相邻段合并。
     */
    public void free(long offset, long size) {
        if (size <= 0) return;

        // 找插入位置——按 offset 升序
        int i = 0;
        while (i < count && offsets[i] < offset) i++;

        boolean mergeLeft  = i > 0 && offsets[i - 1] + sizes[i - 1] == offset;
        boolean mergeRight = i < count && offset + size == offsets[i];

        if (mergeLeft && mergeRight) {
            // 左右都相邻——三者合一
            sizes[i - 1] += size + sizes[i];
            removeAt(i);
        } else if (mergeLeft) {
            sizes[i - 1] += size;
        } else if (mergeRight) {
            offsets[i] = offset;
            sizes[i]  += size;
        } else {
            insertAt(i, offset, size);
        }
    }

    // ═══════════════════════════════════════════════
    // 内部——插入 / 删除
    // ═══════════════════════════════════════════════

    private void insertAt(int i, long offset, long size) {
        ensureCapacity(count + 1);
        // 后移 [i, count)
        System.arraycopy(offsets, i, offsets, i + 1, count - i);
        System.arraycopy(sizes,   i, sizes,   i + 1, count - i);
        offsets[i] = offset;
        sizes[i]   = size;
        count++;
    }

    private void removeAt(int i) {
        int tail = count - i - 1;
        if (tail > 0) {
            System.arraycopy(offsets, i + 1, offsets, i, tail);
            System.arraycopy(sizes,   i + 1, sizes,   i, tail);
        }
        count--;
    }

    private void ensureCapacity(int needed) {
        if (needed <= offsets.length) return;
        int newCap = offsets.length < 1024
                ? offsets.length * 2
                : offsets.length + (offsets.length >> 1);
        long[] no = new long[newCap];
        long[] ns = new long[newCap];
        System.arraycopy(offsets, 0, no, 0, count);
        System.arraycopy(sizes,   0, ns, 0, count);
        offsets = no;
        sizes   = ns;
    }

    // ═══════════════════════════════════════════════
    // 查询
    // ═══════════════════════════════════════════════

    public int  count()     { return count; }
    public boolean isEmpty() { return count == 0; }

    /** 空闲段总数——字节。 */
    public long totalSize() {
        long total = 0;
        for (int i = 0; i < count; i++) total += sizes[i];
        return total;
    }

    /** 调试——第 i 个空闲段的 offset。 */
    public long offsetAt(int i) {
        if (i < 0 || i >= count) throw new IndexOutOfBoundsException();
        return offsets[i];
    }

    /** 调试——第 i 个空闲段的 size。 */
    public long sizeAt(int i) {
        if (i < 0 || i >= count) throw new IndexOutOfBoundsException();
        return sizes[i];
    }

    public void clear() { count = 0; }

    // ═══════════════════════════════════════════════
    // 新增——按位置精确分配
    // ═══════════════════════════════════════════════

    /**
     * 尝试在精确位置 [offset, offset + size) 分配。
     *
     * <p>该区间必须完全被一个空闲段覆盖——否则失败。
     *
     * <p>成功时——从空闲段中切出该区间——不变量保持。
     *
     * @return true——切出成功；false——区间不空闲
     */
    public boolean tryAllocAt(long offset, long size) {
        if (size <= 0) return false;

        // 二分查找——找到 ≤ offset 的最大空闲段
        int i = findSegmentIndex(offset);
        if (i < 0) return false;

        long segStart = offsets[i];
        long segSize  = sizes[i];
        long segEnd   = segStart + segSize;

        // 目标区间必须完全落在该空闲段内
        if (offset < segStart)              return false;
        if (offset + size > segEnd)         return false;

        // 精确命中——切成两段 / 移除
        long leftSize  = offset - segStart;
        long rightSize = segEnd - (offset + size);

        if (leftSize == 0 && rightSize == 0) {
            // 完全命中——移除
            removeAt(i);
        } else if (leftSize == 0) {
            // 左对齐——右段保留
            offsets[i] = offset + size;
            sizes[i]   = rightSize;
        } else if (rightSize == 0) {
            // 右对齐——左段保留
            sizes[i] = leftSize;
        } else {
            // 中间——切成左右两段
            offsets[i] = segStart;
            sizes[i]   = leftSize;
            insertAt(i + 1, offset + size, rightSize);
        }

        return true;
    }

    /**
     * 检查 [offset, offset + size) 是否完全空闲。
     */
    public boolean isFree(long offset, long size) {
        if (size <= 0) return false;

        int i = findSegmentIndex(offset);
        if (i < 0) return false;

        long segStart = offsets[i];
        long segEnd   = segStart + sizes[i];

        return offset >= segStart && offset + size <= segEnd;
    }

    /**
     * 二分查找——返回 ≤ offset 的最大空闲段索引。
     * 无匹配返回 -1。
     */
    private int findSegmentIndex(long offset) {
        int lo = 0;
        int hi = count - 1;
        int result = -1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (offsets[mid] <= offset) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FreeList[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append("(").append(offsets[i])
                    .append(", ").append(sizes[i]).append(")");
        }
        return sb.append(']').toString();
    }
}