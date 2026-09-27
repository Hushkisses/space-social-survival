package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileId;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CompactSingleDeckMapGeneratorTest {

    private final CompactSingleDeckMapGenerator generator =
            new CompactSingleDeckMapGenerator();

    private final MapGenerationConstraints constraints =
            new MapGenerationConstraints(10, 14, 6, 6, 500);

    @Test
    void generatesExactlyTenMeaningfulRoomsAcrossSeeds() {
        for (int seed = 0; seed < 100; seed++) {
            GeneratedMap map = generator.generate(
                    DefaultTileCatalog.create(),
                    constraints,
                    new Random(seed)
            );

            assertEquals(10, map.tileIds().size(), "seed=" + seed);
            assertTrue(map.isConnected(), "seed=" + seed);
            assertTrue(map.deadEndCount() <= 6, "seed=" + seed);

            for (String core : Set.of(
                    "bridge",
                    "engineering",
                    "medical",
                    "research",
                    "cargo",
                    "habitation",
                    "junction_1"
            )) {
                assertEquals(
                        1,
                        map.tileIds().stream()
                                .filter(id -> id.value().equals(core))
                                .count(),
                        "core=" + core + ", seed=" + seed
                );
            }

            assertTrue(
                    map.tileIds().stream()
                            .noneMatch(id -> id.value().startsWith("corridor_")),
                    "corridor room returned, seed=" + seed
            );
            assertFalse(
                    map.tileIds().contains(new TileId("junction_2")),
                    "second hub returned, seed=" + seed
            );
        }
    }

    @Test
    void ejectionValidationSeedContainsAirlock() {
        GeneratedMap map = generator.generate(
                DefaultTileCatalog.create(),
                constraints,
                new Random(1000L)
        );

        assertTrue(
                map.tileIds().contains(new TileId("airlock_1")),
                "seed 1000 must keep an airlock for PX-009 solo validation"
        );
    }

    @Test
    void bridgeToEngineeringStaysShort() {
        for (int seed = 0; seed < 100; seed++) {
            GeneratedMap map = generator.generate(
                    DefaultTileCatalog.create(),
                    constraints,
                    new Random(seed)
            );

            assertTrue(
                    map.distance(new TileId("bridge"), new TileId("engineering")) <= 2,
                    "seed=" + seed
            );
        }
    }
}
