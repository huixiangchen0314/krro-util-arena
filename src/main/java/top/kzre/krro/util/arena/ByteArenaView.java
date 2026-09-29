package top.kzre.krro.util.arena;

public interface ByteArenaView extends ArenaView<ByteArenaView> {
    byte[] getBytes();
    long offset();
    long count();
}