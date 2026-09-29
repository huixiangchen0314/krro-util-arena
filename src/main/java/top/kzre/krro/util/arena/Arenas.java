package top.kzre.krro.util.arena;

import java.util.ArrayList;

public final class Arenas {

    private Arenas() {}

    // ═══════════════════════════════════════════════
    // Float
    // ═══════════════════════════════════════════════

    /** 固定容量——单 storage——初始 floatCount 个 float。 */
    public static FloatArrayArena newSingleFloatArray(int floatCount) {
        ArrayList<FloatArrayStorage> storages = new ArrayList<>();
        storages.add(new FloatArrayStorage(new float[floatCount]));
        return new FloatArrayArena(storages);
    }

    /** 空 arena——不预分配——靠 addStorage 扩充。 */
    public static FloatArrayArena newEmptyFloatArray() {
        return new FloatArrayArena(new ArrayList<>());
    }

    // ═══════════════════════════════════════════════
    // Int
    // ═══════════════════════════════════════════════

    /** 固定容量——单 storage——初始 intCount 个 int。 */
    public static IntArrayArena newSingleIntArray(int intCount) {
        ArrayList<IntArrayStorage> storages = new ArrayList<>();
        storages.add(new IntArrayStorage(new int[intCount]));
        return new IntArrayArena(storages);
    }

    /** 空 arena——不预分配——靠 addStorage 扩充。 */
    public static IntArrayArena newEmptyIntArray() {
        return new IntArrayArena(new ArrayList<>());
    }

}