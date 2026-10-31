package top.kzre.krro.util.arena;

import java.nio.IntBuffer;

public interface IntStorage extends Storage {
    IntBuffer intSlice(long byteOffset, long count);
}