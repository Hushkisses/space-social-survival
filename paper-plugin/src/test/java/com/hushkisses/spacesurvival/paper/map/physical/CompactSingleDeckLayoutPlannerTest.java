package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompactSingleDeckLayoutPlannerTest {

    private final CompactSingleDeckLayoutPlanner planner =
            new CompactSingleDeckLayoutPlanner();

    private static final List<TileId> ALL = List.of(
            new TileId("bridge"),
            new TileId("engineering"),
            new TileId("medical"),
            new TileId("research"),
            new TileId("cargo"),
            new TileId("habitation"),
            new TileId("junction_1"),
            new TileId("auxiliary_1"),
            new TileId("auxiliary_2"),
            new TileId("auxiliary_3"),
            new TileId("auxiliary_4"),
            new TileId("airlock_1")
    );

    @Test
    void allRoomsStayOnOneDeckAndInsideCompactEnvelope() {
        Map<TileId, PhysicalTilePlacement> layout = planner.plan(ALL);

        for (PhysicalTilePlacement placement : layout.values()) {
            assertEquals(CompactSingleDeckLayoutPlanner.FLOOR_Y, placement.floorY());

            int maxX = placement.minX() + placement.size() - 1;
            int maxZ = placement.minZ() + placement.size() - 1;

            assertTrue(placement.minX() >= CompactSingleDeckLayoutPlanner.SHIP_MIN_X);
            assertTrue(maxX <= CompactSingleDeckLayoutPlanner.SHIP_MAX_X);
            assertTrue(placement.minZ() >= CompactSingleDeckLayoutPlanner.SHIP_MIN_Z);
            assertTrue(maxZ <= CompactSingleDeckLayoutPlanner.SHIP_MAX_Z);
        }
    }

    @Test
    void roomsNeverOverlap() {
        List<PhysicalTilePlacement> placements =
                planner.plan(ALL).values().stream().toList();

        for (int i = 0; i < placements.size(); i++) {
            for (int j = i + 1; j < placements.size(); j++) {
                PhysicalTilePlacement first = placements.get(i);
                PhysicalTilePlacement second = placements.get(j);
                assertFalse(
                        overlaps(first, second),
                        first.tileId() + " overlaps " + second.tileId()
                );
            }
        }
    }

    @Test
    void coreFacilitiesExistExactlyOnceInLayout() {
        Map<TileId, PhysicalTilePlacement> layout = planner.plan(List.of(
                new TileId("bridge"),
                new TileId("engineering"),
                new TileId("medical"),
                new TileId("research"),
                new TileId("cargo"),
                new TileId("habitation"),
                new TileId("junction_1")
        ));

        assertEquals(7, layout.size());
        assertEquals(1, count(layout, "bridge"));
        assertEquals(1, count(layout, "engineering"));
        assertEquals(1, count(layout, "medical"));
        assertEquals(1, count(layout, "research"));
        assertEquals(1, count(layout, "cargo"));
        assertEquals(1, count(layout, "habitation"));
    }

    private static long count(
            Map<TileId, PhysicalTilePlacement> layout,
            String id
    ) {
        return layout.keySet().stream()
                .filter(tileId -> tileId.value().equals(id))
                .count();
    }

    private static boolean overlaps(
            PhysicalTilePlacement first,
            PhysicalTilePlacement second
    ) {
        int firstMaxX = first.minX() + first.size() - 1;
        int firstMaxZ = first.minZ() + first.size() - 1;
        int secondMaxX = second.minX() + second.size() - 1;
        int secondMaxZ = second.minZ() + second.size() - 1;

        return first.minX() <= secondMaxX
                && firstMaxX >= second.minX()
                && first.minZ() <= secondMaxZ
                && firstMaxZ >= second.minZ();
    }
}
