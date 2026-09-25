package com.hushkisses.spacesurvival.map.tile;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTileCatalogTest {

    @Test
    void catalogMatchesInitialTwentyTileDesign() {
        List<TileDefinition> tiles = DefaultTileCatalog.create();

        assertEquals(20, tiles.size());
        assertEquals(6, tiles.stream()
                .filter(tile -> tile.category() == TileCategory.CORE)
                .count());
        assertEquals(5, tiles.stream()
                .filter(tile -> tile.category() == TileCategory.CORRIDOR)
                .count());
        assertEquals(3, tiles.stream()
                .filter(tile -> tile.category() == TileCategory.JUNCTION)
                .count());
        assertEquals(2, tiles.stream()
                .filter(tile -> tile.category() == TileCategory.AIRLOCK)
                .count());
        assertEquals(4, tiles.stream()
                .filter(tile -> tile.category() == TileCategory.AUXILIARY)
                .count());
    }

    @Test
    void sixCoreFacilityTilesAreAlwaysPresent() {
        Set<String> coreIds = DefaultTileCatalog.create().stream()
                .filter(tile -> tile.category() == TileCategory.CORE)
                .map(tile -> tile.id().value())
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("bridge", "engineering", "medical", "research", "cargo", "habitation"),
                coreIds
        );
    }
}
