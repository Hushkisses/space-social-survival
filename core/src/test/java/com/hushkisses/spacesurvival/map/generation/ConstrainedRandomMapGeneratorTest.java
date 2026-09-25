package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.tile.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ConstrainedRandomMapGeneratorTest {

    private final ConstrainedRandomMapGenerator generator = new ConstrainedRandomMapGenerator();

    @Test
    void generatesConnectedMapContainingAllCoreTiles() {
        List<TileDefinition> pool = tilePool();
        MapGenerationConstraints constraints =
                new MapGenerationConstraints(10, 12, 4, 4, 100);

        GeneratedMap generated = generator.generate(pool, constraints, new Random(42));

        Set<TileId> coreIds = pool.stream()
                .filter(tile -> tile.category() == TileCategory.CORE)
                .map(TileDefinition::id)
                .collect(Collectors.toSet());

        assertTrue(generated.isConnected());
        assertTrue(generated.tileIds().containsAll(coreIds));
        assertTrue(generated.tileIds().size() >= 10);
        assertTrue(generated.tileIds().size() <= 12);
        assertTrue(generated.deadEndCount() <= 4);
    }

    @Test
    void neverReusesConnectionPoint() {
        GeneratedMap generated = generator.generate(
                tilePool(),
                new MapGenerationConstraints(10, 12, 4, 4, 100),
                new Random(7)
        );

        long endpointCount = generated.connections().stream()
                .flatMap(connection -> java.util.stream.Stream.of(
                        connection.first(),
                        connection.second()
                ))
                .count();

        long uniqueEndpointCount = generated.connections().stream()
                .flatMap(connection -> java.util.stream.Stream.of(
                        connection.first(),
                        connection.second()
                ))
                .distinct()
                .count();

        assertEquals(endpointCount, uniqueEndpointCount);
    }

    @Test
    void generationIsDeterministicForSeed() {
        MapGenerationConstraints constraints =
                new MapGenerationConstraints(10, 12, 4, 4, 100);

        GeneratedMap first = generator.generate(tilePool(), constraints, new Random(12345));
        GeneratedMap second = generator.generate(tilePool(), constraints, new Random(12345));

        assertEquals(first.tileIds(), second.tileIds());
        assertEquals(first.connections(), second.connections());
    }

    @Test
    void rejectsPoolWithoutCoreTile() {
        List<TileDefinition> pool = List.of(
                tile("corridor", TileCategory.CORRIDOR, 3),
                tile("junction", TileCategory.JUNCTION, 3)
        );

        assertThrows(
                MapGenerationException.class,
                () -> generator.generate(
                        pool,
                        new MapGenerationConstraints(2, 2, 2, 3, 10),
                        new Random(1)
                )
        );
    }

    @Test
    void impossibleConnectionCapacityFailsClearly() {
        List<TileDefinition> pool = List.of(
                tile("core_a", TileCategory.CORE, 1),
                tile("core_b", TileCategory.CORE, 1),
                tile("aux", TileCategory.AUXILIARY, 1)
        );

        assertThrows(
                MapGenerationException.class,
                () -> generator.generate(
                        pool,
                        new MapGenerationConstraints(3, 3, 3, 5, 5),
                        new Random(1)
                )
        );
    }

    private static List<TileDefinition> tilePool() {
        ArrayList<TileDefinition> tiles = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            tiles.add(tile("core_" + i, TileCategory.CORE, 4));
        }
        for (int i = 1; i <= 5; i++) {
            tiles.add(tile("corridor_" + i, TileCategory.CORRIDOR, 3));
        }
        for (int i = 1; i <= 3; i++) {
            tiles.add(tile("junction_" + i, TileCategory.JUNCTION, 4));
        }
        for (int i = 1; i <= 2; i++) {
            tiles.add(tile("airlock_" + i, TileCategory.AIRLOCK, 3));
        }
        for (int i = 1; i <= 4; i++) {
            tiles.add(tile("aux_" + i, TileCategory.AUXILIARY, 3));
        }

        return tiles;
    }

    private static TileDefinition tile(String id, TileCategory category, int pointCount) {
        ArrayList<ConnectionPointDefinition> points = new ArrayList<>();
        for (int i = 1; i <= pointCount; i++) {
            points.add(new ConnectionPointDefinition(
                    new ConnectionPointId("p" + i),
                    ConnectionPointType.TELEPORT
            ));
        }

        return new TileDefinition(
                new TileId(id),
                category,
                id,
                points
        );
    }
}
