package top.kzre.krro.util.arena;

import java.nio.IntBuffer;

public interface IntArenaView extends ArenaView<IntArenaView> {
    int[] getInts();
    long offset();
    long count();
    IntBuffer intBuffer();
}
