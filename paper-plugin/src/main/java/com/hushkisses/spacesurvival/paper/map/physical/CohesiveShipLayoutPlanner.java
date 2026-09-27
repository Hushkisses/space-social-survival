package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;

public final class CohesiveShipLayoutPlanner {

    public static final int MODULE_SIZE = 13;
    public static final int LOWER_FLOOR_Y = 80;
    public static final int UPPER_FLOOR_Y = 88;

    private static final Map<String, Slot> SEMANTIC_SLOTS = semanticSlots();

    public Map<TileId, PhysicalTilePlacement> plan(
            Collection<TileId> selected,
            long seed
    ) {
        Objects.requireNonNull(selected, "selected");

        LinkedHashMap<TileId, PhysicalTilePlacement> placements = new LinkedHashMap<>();
        ArrayList<TileId> fallback = new ArrayList<>();

        for (TileId tileId : selected) {
            Slot slot = SEMANTIC_SLOTS.get(tileId.value());
            if (slot == null) {
                fallback.add(tileId);
                continue;
            }
            placements.put(tileId, placement(tileId, slot));
        }

        if (!fallback.isEmpty()) {
            ArrayList<Slot> available = new ArrayList<>(fallbackSlots());
            Collections.shuffle(available, new Random(seed ^ 0x51A71A9DL));

            int index = 0;
            for (TileId tileId : fallback) {
                Slot slot = index < available.size()
                        ? available.get(index++)
                        : new Slot(90 + index * 18, LOWER_FLOOR_Y, 0);
                placements.put(tileId, placement(tileId, slot));
            }
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
                MODULE_SIZE
        );
    }

    private static Map<String, Slot> semanticSlots() {
        LinkedHashMap<String, Slot> slots = new LinkedHashMap<>();

        // 상부 데크: 지휘 / 생활 / 의료 / 연구
        slots.put("bridge", new Slot(0, UPPER_FLOOR_Y, 0));
        slots.put("corridor_1", new Slot(18, UPPER_FLOOR_Y, 0));
        slots.put("junction_1", new Slot(36, UPPER_FLOOR_Y, 0));
        slots.put("habitation", new Slot(54, UPPER_FLOOR_Y, -18));
        slots.put("medical", new Slot(54, UPPER_FLOOR_Y, 18));
        slots.put("research", new Slot(72, UPPER_FLOOR_Y, 18));
        slots.put("corridor_4", new Slot(72, UPPER_FLOOR_Y, -18));
        slots.put("corridor_5", new Slot(90, UPPER_FLOOR_Y, 18));
        slots.put("auxiliary_1", new Slot(90, UPPER_FLOOR_Y, -18));
        slots.put("auxiliary_2", new Slot(72, UPPER_FLOOR_Y, 36));
        slots.put("airlock_1", new Slot(108, UPPER_FLOOR_Y, -18));

        // 하부 데크: 화물 / 기관 / 에어록 / 정비
        // junction_2 is directly under junction_1 and becomes the central stairwell.
        slots.put("junction_2", new Slot(36, LOWER_FLOOR_Y, 0));
        slots.put("cargo", new Slot(54, LOWER_FLOOR_Y, -18));
        slots.put("corridor_2", new Slot(54, LOWER_FLOOR_Y, 0));
        slots.put("engineering", new Slot(72, LOWER_FLOOR_Y, 0));
        slots.put("corridor_3", new Slot(90, LOWER_FLOOR_Y, 0));
        slots.put("junction_3", new Slot(72, LOWER_FLOOR_Y, -18));
        slots.put("auxiliary_3", new Slot(90, LOWER_FLOOR_Y, -18));
        slots.put("auxiliary_4", new Slot(90, LOWER_FLOOR_Y, 18));
        slots.put("airlock_2", new Slot(108, LOWER_FLOOR_Y, 0));

        return Map.copyOf(slots);
    }

    private static List<Slot> fallbackSlots() {
        return List.of(
                new Slot(108, UPPER_FLOOR_Y, 18),
                new Slot(108, UPPER_FLOOR_Y, 36),
                new Slot(108, LOWER_FLOOR_Y, 18),
                new Slot(108, LOWER_FLOOR_Y, -18)
        );
    }

    private record Slot(int x, int floorY, int z) {
    }
}
