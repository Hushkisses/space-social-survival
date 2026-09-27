package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;

public final class CohesiveShipLayoutPlanner {

    public static final int LOWER_FLOOR_Y = 80;
    public static final int UPPER_FLOOR_Y = 88;

    public static final int SHIP_MIN_X = 0;
    public static final int SHIP_MAX_X = 56;
    public static final int SHIP_MIN_Z = -24;
    public static final int SHIP_MAX_Z = 24;

    private static final Map<String, Slot> SEMANTIC_SLOTS = semanticSlots();

    public Map<TileId, PhysicalTilePlacement> plan(
            Collection<TileId> selected,
            long seed
    ) {
        Objects.requireNonNull(selected, "selected");

        LinkedHashMap<TileId, PhysicalTilePlacement> placements = new LinkedHashMap<>();

        for (TileId tileId : selected) {
            Slot slot = SEMANTIC_SLOTS.get(tileId.value());
            if (slot == null) {
                throw new IllegalArgumentException(
                        "No compact physical slot for tile: " + tileId.value()
                );
            }
            placements.put(tileId, placement(tileId, slot));
        }

        return Collections.unmodifiableMap(placements);
    }

    public static boolean upperDeck(TileId tileId) {
        Slot slot = SEMANTIC_SLOTS.get(tileId.value());
        return slot != null && slot.floorY == UPPER_FLOOR_Y;
    }

    private static PhysicalTilePlacement placement(TileId tileId, Slot slot) {
        return new PhysicalTilePlacement(
                tileId,
                slot.x,
                slot.floorY,
                slot.z,
                slot.size
        );
    }

    private static Map<String, Slot> semanticSlots() {
        LinkedHashMap<String, Slot> slots = new LinkedHashMap<>();

        // Upper deck: command / habitation / medicine / research.
        // Total footprint stays inside roughly 53 x 42 blocks.
        slots.put("bridge", new Slot(0, UPPER_FLOOR_Y, -5, 11));
        slots.put("junction_1", new Slot(16, UPPER_FLOOR_Y, -5, 11));
        slots.put("habitation", new Slot(16, UPPER_FLOOR_Y, -20, 11));
        slots.put("medical", new Slot(16, UPPER_FLOOR_Y, 10, 11));
        slots.put("research", new Slot(32, UPPER_FLOOR_Y, 10, 11));

        slots.put("auxiliary_1", new Slot(32, UPPER_FLOOR_Y, -18, 9));
        slots.put("auxiliary_2", new Slot(44, UPPER_FLOOR_Y, -18, 9));
        slots.put("airlock_1", new Slot(44, UPPER_FLOOR_Y, 0, 9));

        // Lower deck: cargo / engineering / maintenance / airlock.
        // junction_2 is directly below junction_1 and forms the stair/atrium core.
        slots.put("junction_2", new Slot(16, LOWER_FLOOR_Y, -5, 11));
        slots.put("cargo", new Slot(16, LOWER_FLOOR_Y, -21, 13));
        slots.put("engineering", new Slot(32, LOWER_FLOOR_Y, -5, 13));

        slots.put("auxiliary_3", new Slot(32, LOWER_FLOOR_Y, -21, 9));
        slots.put("auxiliary_4", new Slot(47, LOWER_FLOOR_Y, -5, 9));
        slots.put("airlock_2", new Slot(32, LOWER_FLOOR_Y, 10, 9));

        return Map.copyOf(slots);
    }

    private record Slot(int x, int floorY, int z, int size) {
    }
}
