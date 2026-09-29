package top.kzre.krro.util.arena;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 抽象 ArenaView——维护引用计数。
 *
 * <p><b>引用计数</b>：初始为 1，所有权归申请者。调用方通过
 * {@link #acquire()} / {@link #release()} 管理共享。归零时触发
 * {@link #onRelease()}——子类实现释放逻辑。
 *
 * <p><b>线程契约</b>：引用计数是原子的——`acquire` / `release`
 * 可并发。其他方法非线程安全——由调用方保证。
 *
 * @param <T> 具体视图类型
 */
public abstract class AbstractArenaView<T extends ArenaView<T>>
        implements ArenaView<T> {

    /** 引用计数——初始为 1。 */
    private final AtomicInteger refCount = new AtomicInteger(1);

    // ═══════════════════════════════════════════════
    // 引用计数
    // ═══════════════════════════════════════════════

    @Override
    public int acquire() {
        return refCount.incrementAndGet();
    }

    @Override
    public int release() {
        int remaining = refCount.decrementAndGet();
        if (remaining == 0) {
            onRelease();
        } else if (remaining < 0) {
            throw new IllegalStateException(
                    "release called more times than acquire: " + refCount.get());
        }
        return remaining;
    }

    @Override
    public int refCount() {
        return refCount.get();
    }

    /**
     * 引用计数归零时触发——子类实现释放逻辑。
     *
     * <p>只调用一次——`release` 保证。
     */
    protected abstract void onRelease();
}