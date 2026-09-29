package top.kzre.krro.util.arena;

import java.util.ArrayList;

public final class Arenas {

    private Arenas() {}

    public static FloatArrayArena newSingleFloatArray(int floatCount) {
        ArrayList<FloatArrayStorage> storages = new ArrayList<>();
        storages.add(new FloatArrayStorage(new float[floatCount]));
        return new FloatArrayArena(storages);
    }

    public static IntArrayArena newSingleIntArray(int intCount) {
        ArrayList<IntArrayStorage> storages = new ArrayList<>();
        storages.add(new IntArrayStorage(new int[intCount]));
        return new IntArrayArena(storages);
    }

}