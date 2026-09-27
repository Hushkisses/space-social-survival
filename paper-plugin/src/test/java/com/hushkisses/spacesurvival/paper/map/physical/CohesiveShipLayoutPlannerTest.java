package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CohesiveShipLayoutPlannerTest {

    private final CohesiveShipLayoutPlanner planner = new CohesiveShipLayoutPlanner();

    @Test
    void semanticCoreFacilitiesHaveIntuitiveFrontToRearOrder() {
        List<TileId> ids = DefaultTileCatalog.create().stream()
                .map(tile -> tile.id())
                .toList();

        Map<TileId, PhysicalTilePlacement> layout = planner.plan(ids, 1004L);

        PhysicalTilePlacement bridge = layout.get(new TileId("bridge"));
        PhysicalTilePlacement habitation = layout.get(new TileId("habitation"));
        PhysicalTilePlacement cargo = layout.get(new TileId("cargo"));
        PhysicalTilePlacement engineering = layout.get(new TileId("engineering"));

        assertTrue(bridge.minX() < habitation.minX());
        assertTrue(habitation.minX() <= cargo.minX());
        assertTrue(cargo.minX() < engineering.minX());

        assertEquals(CohesiveShipLayoutPlanner.UPPER_FLOOR_Y, bridge.floorY());
        assertEquals(CohesiveShipLayoutPlanner.UPPER_FLOOR_Y, habitation.floorY());
        assertEquals(CohesiveShipLayoutPlanner.LOWER_FLOOR_Y, cargo.floorY());
        assertEquals(CohesiveShipLayoutPlanner.LOWER_FLOOR_Y, engineering.floorY());
    }

    @Test
    void allKnownModuleSlotsRemainPhysicallySeparated() {
        List<TileId> ids = DefaultTileCatalog.create().stream()
                .map(tile -> tile.id())
                .toList();

        Map<TileId, PhysicalTilePlacement> layout = planner.plan(ids, 1004L);
        List<PhysicalTilePlacement> placements = layout.values().stream().toList();

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
    void plannerIsStableForTheSameSelectedModules() {
        List<TileId> ids = List.of(
                new TileId("bridge"),
                new TileId("engineering"),
                new TileId("medical"),
                new TileId("research"),
                new TileId("cargo"),
                new TileId("habitation"),
                new TileId("auxiliary_1"),
                new TileId("airlock_1")
        );

        assertEquals(
                planner.plan(ids, 4242L),
                planner.plan(ids, 4242L)
        );
    }

    private static boolean overlaps(
            PhysicalTilePlacement first,
            PhysicalTilePlacement second
    ) {
        int firstMaxX = first.minX() + first.size() - 1;
        int firstMaxZ = first.minZ() + first.size() - 1;
        int secondMaxX = second.minX() + second.size() - 1;
        int secondMaxZ = second.minZ() + second.size() - 1;

        int firstMaxY = first.floorY() + 6;
        int secondMaxY = second.floorY() + 6;

        return first.minX() <= secondMaxX
                && firstMaxX >= second.minX()
                && first.minZ() <= secondMaxZ
                && firstMaxZ >= second.minZ()
                && first.floorY() <= secondMaxY
                && firstMaxY >= second.floorY();
    }
}
