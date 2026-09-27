package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.TileId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CohesiveShipLayoutPlannerTest {

    private final CohesiveShipLayoutPlanner planner = new CohesiveShipLayoutPlanner();

    private static final List<TileId> ALL_COMPACT_TILES = List.of(
            new TileId("bridge"),
            new TileId("engineering"),
            new TileId("medical"),
            new TileId("research"),
            new TileId("cargo"),
            new TileId("habitation"),
            new TileId("junction_1"),
            new TileId("junction_2"),
            new TileId("auxiliary_1"),
            new TileId("auxiliary_2"),
            new TileId("auxiliary_3"),
            new TileId("auxiliary_4"),
            new TileId("airlock_1"),
            new TileId("airlock_2")
    );

    @Test
    void coreFacilitiesUseTwoFunctionalDecks() {
        Map<TileId, PhysicalTilePlacement> layout =
                planner.plan(ALL_COMPACT_TILES, 1004L);

        assertEquals(
                CohesiveShipLayoutPlanner.UPPER_FLOOR_Y,
                layout.get(new TileId("bridge")).floorY()
        );
        assertEquals(
                CohesiveShipLayoutPlanner.UPPER_FLOOR_Y,
                layout.get(new TileId("habitation")).floorY()
        );
        assertEquals(
                CohesiveShipLayoutPlanner.UPPER_FLOOR_Y,
                layout.get(new TileId("medical")).floorY()
        );
        assertEquals(
                CohesiveShipLayoutPlanner.UPPER_FLOOR_Y,
                layout.get(new TileId("research")).floorY()
        );

        assertEquals(
                CohesiveShipLayoutPlanner.LOWER_FLOOR_Y,
                layout.get(new TileId("cargo")).floorY()
        );
        assertEquals(
                CohesiveShipLayoutPlanner.LOWER_FLOOR_Y,
                layout.get(new TileId("engineering")).floorY()
        );
    }

    @Test
    void upperAndLowerAtriumShareHorizontalFootprint() {
        Map<TileId, PhysicalTilePlacement> layout =
                planner.plan(ALL_COMPACT_TILES, 1004L);

        PhysicalTilePlacement upper = layout.get(new TileId("junction_1"));
        PhysicalTilePlacement lower = layout.get(new TileId("junction_2"));

        assertEquals(upper.minX(), lower.minX());
        assertEquals(upper.minZ(), lower.minZ());
        assertEquals(upper.size(), lower.size());
        assertNotEquals(upper.floorY(), lower.floorY());
    }

    @Test
    void entireShipFitsCompactPlaytestEnvelope() {
        Map<TileId, PhysicalTilePlacement> layout =
                planner.plan(ALL_COMPACT_TILES, 1004L);

        for (PhysicalTilePlacement placement : layout.values()) {
            int maxX = placement.minX() + placement.size() - 1;
            int maxZ = placement.minZ() + placement.size() - 1;

            assertTrue(
                    placement.minX() >= CohesiveShipLayoutPlanner.SHIP_MIN_X,
                    placement.tileId() + " minX"
            );
            assertTrue(
                    maxX <= CohesiveShipLayoutPlanner.SHIP_MAX_X,
                    placement.tileId() + " maxX"
            );
            assertTrue(
                    placement.minZ() >= CohesiveShipLayoutPlanner.SHIP_MIN_Z,
                    placement.tileId() + " minZ"
            );
            assertTrue(
                    maxZ <= CohesiveShipLayoutPlanner.SHIP_MAX_Z,
                    placement.tileId() + " maxZ"
            );
        }
    }

    @Test
    void roomsDoNotOverlapOnTheSameDeck() {
        Map<TileId, PhysicalTilePlacement> layout =
                planner.plan(ALL_COMPACT_TILES, 1004L);
        List<PhysicalTilePlacement> placements = layout.values().stream().toList();

        for (int i = 0; i < placements.size(); i++) {
            for (int j = i + 1; j < placements.size(); j++) {
                PhysicalTilePlacement first = placements.get(i);
                PhysicalTilePlacement second = placements.get(j);

                if (first.floorY() != second.floorY()) {
                    continue;
                }

                assertFalse(
                        overlaps2d(first, second),
                        first.tileId() + " overlaps " + second.tileId()
                );
            }
        }
    }

    private static boolean overlaps2d(
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
