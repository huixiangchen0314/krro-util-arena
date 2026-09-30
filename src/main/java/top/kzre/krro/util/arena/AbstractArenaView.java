package top.kzre.krro.util.arena;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 引用计数视图基类。
 *
 * <p>release 归零时调 {@link #onRelease()} 归还段。
 */
public abstract class AbstractArenaView<T extends ArenaView<T>>
        implements ArenaView<T> {

    private final AtomicInteger refCount = new AtomicInteger(1);

    @Override
    public int acquire() {
        return refCount.incrementAndGet();
    }

    @Override
    public int release() {
        int remaining = refCount.decrementAndGet();
        if (remaining < 0) {
            throw new IllegalStateException(
                    "release called too many times: refCount=" + remaining);
        }
        if (remaining == 0) {
            onRelease();
        }
        return remaining;
    }

    @Override
    public int refCount() {
        return refCount.get();
    }

    /** 引用归零——归还段。 */
    protected abstract void onRelease();
}