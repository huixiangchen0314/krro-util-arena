package top.kzre.krro.util.arena;

/**
 * 分配失败的原因。
 */
public enum AllocateError {
    NO_ERROR,
    /** Arena 已满——无空闲段。 */
    OUT_OF_MEMORY,

    /** pack 后仍不足——碎片严重。 */
    FRAGMENTED,

    /** 其他。 */
    UNKNOWN
}