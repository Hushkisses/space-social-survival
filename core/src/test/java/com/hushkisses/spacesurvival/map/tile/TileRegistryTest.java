package com.hushkisses.spacesurvival.map.tile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TileRegistryTest {

    @Test
    void registersAndLooksUpTileDefinition() {
        TileRegistry registry = new TileRegistry();
        TileDefinition tile = engineeringTile();

        registry.register(tile);

        assertSame(tile, registry.require(new TileId("engineering")));
        assertEquals(1, registry.size());
        assertEquals(List.of(tile), registry.byCategory(TileCategory.CORE));
    }

    @Test
    void duplicateTileIdIsRejected() {
        TileRegistry registry = new TileRegistry();
        registry.register(engineeringTile());

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(engineeringTile())
        );
    }

    @Test
    void duplicateConnectionPointInsideTileIsRejected() {
        ConnectionPointDefinition north =
                new ConnectionPointDefinition(new ConnectionPointId("north"), ConnectionPointType.DOOR);

        assertThrows(
                IllegalArgumentException.class,
                () -> new TileDefinition(
                        new TileId("bad"),
                        TileCategory.CORRIDOR,
                        "잘못된 타일",
                        List.of(north, north)
                )
        );
    }

    @Test
    void connectionPointCanBeQueried() {
        TileDefinition tile = engineeringTile();

        ConnectionPointDefinition north =
                tile.connectionPoint(new ConnectionPointId("north")).orElseThrow();

        assertEquals(ConnectionPointType.AIRLOCK, north.type());
    }

    @Test
    void unknownTileRequireFails() {
        TileRegistry registry = new TileRegistry();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.require(new TileId("missing"))
        );
    }

    private static TileDefinition engineeringTile() {
        return new TileDefinition(
                new TileId("engineering"),
                TileCategory.CORE,
                "기관 구역",
                List.of(
                        new ConnectionPointDefinition(
                                new ConnectionPointId("north"),
                                ConnectionPointType.AIRLOCK
                        ),
                        new ConnectionPointDefinition(
                                new ConnectionPointId("south"),
                                ConnectionPointType.DOOR
                        )
                )
        );
    }
}
