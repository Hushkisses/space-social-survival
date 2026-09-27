package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;

public final class CompactSingleDeckLayoutPlanner {

    public static final int FLOOR_Y = 80;

    public static final int SHIP_MIN_X = 0;
    public static final int SHIP_MAX_X = 49;
    public static final int SHIP_MIN_Z = -19;
    public static final int SHIP_MAX_Z = 19;

    private static final Map<String, Slot> SLOTS = slots();

    public Map<TileId, PhysicalTilePlacement> plan(Collection<TileId> selected) {
        Objects.requireNonNull(selected, "selected");

        LinkedHashMap<TileId, PhysicalTilePlacement> placements = new LinkedHashMap<>();

        for (TileId tileId : selected) {
            Slot slot = SLOTS.get(tileId.value());
            if (slot == null) {
                throw new IllegalArgumentException(
                        "No MAP-V4 slot for tile: " + tileId.value()
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

        // Main skeleton. The hub sits in the middle; every core facility is within
        // one short branch or side loop.
        slots.put("bridge", new Slot(0, -4, 9));
        slots.put("junction_1", new Slot(12, -5, 11));

        slots.put("habitation", new Slot(12, -17, 9));
        slots.put("medical", new Slot(12, 9, 9));

        slots.put("cargo", new Slot(25, -17, 11));
        slots.put("research", new Slot(25, 9, 9));
        slots.put("engineering", new Slot(28, -5, 11));

        // Only three of these five are present in any match.
        slots.put("auxiliary_1", new Slot(0, -17, 9));
        slots.put("auxiliary_2", new Slot(0, 9, 9));
        slots.put("auxiliary_3", new Slot(39, -17, 9));
        slots.put("auxiliary_4", new Slot(37, 9, 9));
        slots.put("airlock_1", new Slot(41, -4, 9));

        return Map.copyOf(slots);
    }

    private record Slot(int x, int z, int size) {
    }
}
