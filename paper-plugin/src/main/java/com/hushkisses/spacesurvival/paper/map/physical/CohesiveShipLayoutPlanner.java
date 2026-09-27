package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;

public final class CohesiveShipLayoutPlanner {

    public static final int MODULE_SIZE = 15;
    public static final int FLOOR_Y = 80;

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
                        : new Slot(168 + index * 24, 0);
                placements.put(tileId, placement(tileId, slot));
            }
        }

        return Collections.unmodifiableMap(placements);
    }

    private static PhysicalTilePlacement placement(TileId tileId, Slot slot) {
        return new PhysicalTilePlacement(
                tileId,
                slot.x,
                FLOOR_Y,
                slot.z,
                MODULE_SIZE
        );
    }

    private static Map<String, Slot> semanticSlots() {
        LinkedHashMap<String, Slot> slots = new LinkedHashMap<>();

        // 선수 / 지휘 구역
        slots.put("bridge", new Slot(0, 0));
        slots.put("corridor_1", new Slot(24, 0));
        slots.put("junction_1", new Slot(48, 0));

        // 중앙 생활 / 의료 구역
        slots.put("habitation", new Slot(48, -36));
        slots.put("medical", new Slot(48, 36));
        slots.put("corridor_4", new Slot(72, -36));
        slots.put("corridor_5", new Slot(72, 36));

        // 중앙-후방 연결 구역
        slots.put("corridor_2", new Slot(72, 0));
        slots.put("junction_2", new Slot(96, 0));

        // 후방 작업 / 연구 구역
        slots.put("cargo", new Slot(108, -36));
        slots.put("research", new Slot(108, 36));
        slots.put("corridor_3", new Slot(120, 0));
        slots.put("engineering", new Slot(144, 0));

        // 외곽 정비 / 탐험 구역
        slots.put("auxiliary_1", new Slot(36, -72));
        slots.put("auxiliary_2", new Slot(36, 72));
        slots.put("junction_3", new Slot(84, -72));
        slots.put("auxiliary_3", new Slot(108, -72));
        slots.put("auxiliary_4", new Slot(108, 72));
        slots.put("airlock_1", new Slot(144, -72));
        slots.put("airlock_2", new Slot(144, 72));

        return Map.copyOf(slots);
    }

    private static List<Slot> fallbackSlots() {
        return List.of(
                new Slot(12, -72),
                new Slot(12, 72),
                new Slot(60, -72),
                new Slot(60, 72),
                new Slot(132, -36),
                new Slot(132, 36)
        );
    }

    private record Slot(int x, int z) {
    }
}
