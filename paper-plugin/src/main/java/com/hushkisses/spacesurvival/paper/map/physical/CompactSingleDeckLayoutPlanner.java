package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;

public final class CompactSingleDeckLayoutPlanner {

    public static final int FLOOR_Y = 80;

    public static final int SHIP_MIN_X = 0;
    public static final int SHIP_MAX_X = 60;
    public static final int SHIP_MIN_Z = -23;
    public static final int SHIP_MAX_Z = 23;

    private static final Map<String, Slot> SLOTS = slots();

    public Map<TileId, PhysicalTilePlacement> plan(Collection<TileId> selected) {
        Objects.requireNonNull(selected, "selected");

        LinkedHashMap<TileId, PhysicalTilePlacement> placements = new LinkedHashMap<>();

        for (TileId tileId : selected) {
            Slot slot = SLOTS.get(tileId.value());
            if (slot == null) {
                throw new IllegalArgumentException(
                        "No MAP-V5 slot for tile: " + tileId.value()
                );
            }
            placements.put(
                    tileId,
                    new PhysicalTilePlacement(
                            tileId,
                            slot.x,
                            FLOOR_Y,
                            slot.z,
                            slot.size
                    )
            );
        }

        return Collections.unmodifiableMap(placements);
    }

    private static Map<String, Slot> slots() {
        LinkedHashMap<String, Slot> slots = new LinkedHashMap<>();

        // MAP-V5: restore generous room sizes, but keep rooms close together.
        // The gaps between connected core rooms are generally 1-3 blocks.
        slots.put("bridge", new Slot(0, -7, 15));
        slots.put("junction_1", new Slot(18, -5, 11));
        slots.put("engineering", new Slot(32, -7, 15));

        slots.put("habitation", new Slot(18, -21, 13));
        slots.put("cargo", new Slot(34, -23, 13));

        slots.put("medical", new Slot(18, 9, 13));
        slots.put("research", new Slot(34, 11, 13));

        // Exactly three of these are selected. They sit directly outside the
        // core rooms rather than creating another corridor layer.
        slots.put("auxiliary_1", new Slot(4, -23, 11));
        slots.put("auxiliary_2", new Slot(4, 13, 11));
        slots.put("auxiliary_3", new Slot(50, -23, 11));
        slots.put("auxiliary_4", new Slot(50, 13, 11));
        slots.put("airlock_1", new Slot(50, -5, 11));

        return Map.copyOf(slots);
    }

    private record Slot(int x, int z, int size) {
    }
}
