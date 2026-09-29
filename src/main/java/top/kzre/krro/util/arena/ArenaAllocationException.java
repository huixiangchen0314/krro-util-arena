package top.kzre.krro.util.arena;

/**
 * Arena 分配异常——请求超过单页上限 / 存储耗尽且无法扩容时抛出。
 *
 * <p><b>非受检</b>：{@code RuntimeException} 子类——调用方无需显式
 * catch。这类异常通常表示编程错误或资源上限——不是业务可恢复路径。
 */
public class ArenaAllocationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ArenaAllocationException() { super(); }
    public ArenaAllocationException(String message) { super(message); }
    public ArenaAllocationException(String message, Throwable cause) { super(message, cause); }
    public ArenaAllocationException(Throwable cause) { super(cause); }
}