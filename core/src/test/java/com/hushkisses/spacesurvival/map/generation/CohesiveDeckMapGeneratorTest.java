package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileId;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CohesiveDeckMapGeneratorTest {

    private final CohesiveDeckMapGenerator generator = new CohesiveDeckMapGenerator();
    private final MapGenerationConstraints constraints =
            new MapGenerationConstraints(10, 14, 6, 6, 500);

    @Test
    void generatesCompactConnectedTopologyAcrossSeeds() {
        for (int seed = 0; seed < 100; seed++) {
            GeneratedMap generated = generator.generate(
                    DefaultTileCatalog.create(),
                    constraints,
                    new Random(seed)
            );

            assertTrue(generated.isConnected(), "seed=" + seed);
            assertTrue(generated.tileIds().size() >= 10, "seed=" + seed);
            assertTrue(generated.tileIds().size() <= 12, "seed=" + seed);
            assertTrue(generated.deadEndCount() <= 6, "seed=" + seed);

            for (TileId core : Set.of(
                    new TileId("bridge"),
                    new TileId("engineering"),
                    new TileId("medical"),
                    new TileId("research"),
                    new TileId("cargo"),
                    new TileId("habitation"),
                    new TileId("junction_1"),
                    new TileId("junction_2")
            )) {
                assertTrue(generated.tileIds().contains(core), "seed=" + seed);
            }

            assertTrue(
                    generated.tileIds().stream()
                            .noneMatch(id -> id.value().startsWith("corridor_")),
                    "independent corridor rooms must not return, seed=" + seed
            );
        }
    }

    @Test
    void usesSingleVerticalSpine() {
        GeneratedMap generated = generator.generate(
                DefaultTileCatalog.create(),
                constraints,
                new Random(1004)
        );

        long crossDeck = generated.connections().stream()
                .filter(connection ->
                        upper(connection.first().tileId())
                                != upper(connection.second().tileId())
                )
                .count();

        assertEquals(1, crossDeck);
        assertTrue(generated.connections().stream().anyMatch(connection ->
                pair(connection, "junction_1", "junction_2")
        ));
    }

    @Test
    void neverReusesConnectionPoints() {
        for (int seed = 0; seed < 100; seed++) {
            int currentSeed = seed;
            GeneratedMap generated = generator.generate(
                    DefaultTileCatalog.create(),
                    constraints,
                    new Random(currentSeed)
            );

            Set<Object> endpoints = new HashSet<>();
            generated.connections().forEach(connection -> {
                assertTrue(endpoints.add(connection.first()), "seed=" + currentSeed);
                assertTrue(endpoints.add(connection.second()), "seed=" + currentSeed);
            });
        }
    }

    private static boolean pair(
            GeneratedConnection connection,
            String a,
            String b
    ) {
        String first = connection.first().tileId().value();
        String second = connection.second().tileId().value();
        return (first.equals(a) && second.equals(b))
                || (first.equals(b) && second.equals(a));
    }

    private static boolean upper(TileId id) {
        return Set.of(
                "bridge",
                "medical",
                "research",
                "habitation",
                "junction_1",
                "auxiliary_1",
                "auxiliary_2",
                "airlock_1"
        ).contains(id.value());
    }
}
