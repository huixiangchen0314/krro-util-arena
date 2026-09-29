package top.kzre.krro.util.arena;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class IntArrayArenaTemplate
        extends AbstractArenaTemplate<IntArrayArena, IntArenaView> {

    public IntArrayArenaTemplate(IntArrayArena arena) {
        super(arena);
    }

    @Override
    protected List<Storage> priorityStorage(List<Storage> storages) {
        if (storages.size() < 2) return storages;

        List<Storage> sorted = new ArrayList<>(storages);
        sorted.sort(Comparator.comparingLong(Storage::capacity).reversed());
        return sorted;
    }
}