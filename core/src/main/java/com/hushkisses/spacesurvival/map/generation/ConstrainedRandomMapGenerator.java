package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.connection.TileConnectionPointRef;
import com.hushkisses.spacesurvival.map.tile.ConnectionPointDefinition;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ConstrainedRandomMapGenerator {

    public GeneratedMap generate(
            Collection<TileDefinition> candidates,
            MapGenerationConstraints constraints,
            RandomGenerator random
    ) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(constraints, "constraints");
        Objects.requireNonNull(random, "random");

        List<TileDefinition> pool = List.copyOf(candidates);
        validatePool(pool, constraints);

        for (int attempt = 0; attempt < constraints.maxAttempts(); attempt++) {
            GeneratedMap generated = tryGenerate(pool, constraints, random);
            if (generated != null && satisfies(generated, pool, constraints)) {
                return generated;
            }
        }

        throw new MapGenerationException(
                "Could not generate map within " + constraints.maxAttempts() + " attempts"
        );
    }

    private GeneratedMap tryGenerate(
            List<TileDefinition> pool,
            MapGenerationConstraints constraints,
            RandomGenerator random
    ) {
        int targetSize = random.nextInt(
                constraints.minTiles(),
                constraints.maxTiles() + 1
        );

        List<TileDefinition> coreTiles = pool.stream()
                .filter(tile -> tile.category() == TileCategory.CORE)
                .toList();

        if (coreTiles.size() > targetSize) {
            return null;
        }

        ArrayList<TileDefinition> optional = new ArrayList<>(
                pool.stream()
                        .filter(tile -> tile.category() != TileCategory.CORE)
                        .toList()
        );
        shuffle(optional, random);

        ArrayList<TileDefinition> selected = new ArrayList<>(coreTiles);
        for (TileDefinition tile : optional) {
            if (selected.size() >= targetSize) {
                break;
            }
            selected.add(tile);
        }

        if (selected.size() != targetSize) {
            return null;
        }

        shuffle(selected, random);

        Map<TileId, ArrayDeque<ConnectionPointDefinition>> unusedPoints = new HashMap<>();
        for (TileDefinition tile : selected) {
            if (tile.connectionPoints().isEmpty()) {
                return null;
            }
            ArrayList<ConnectionPointDefinition> points = new ArrayList<>(tile.connectionPoints());
            shuffle(points, random);
            unusedPoints.put(tile.id(), new ArrayDeque<>(points));
        }

        ArrayList<GeneratedConnection> connections = new ArrayList<>();
        LinkedHashSet<TileId> connected = new LinkedHashSet<>();
        connected.add(selected.getFirst().id());

        for (int i = 1; i < selected.size(); i++) {
            TileDefinition next = selected.get(i);

            List<TileId> possibleParents = connected.stream()
                    .filter(id -> !unusedPoints.get(id).isEmpty())
                    .toList();

            if (possibleParents.isEmpty() || unusedPoints.get(next.id()).isEmpty()) {
                return null;
            }

            TileId parent = possibleParents.get(random.nextInt(possibleParents.size()));
            GeneratedConnection connection = connect(parent, next.id(), unusedPoints);
            connections.add(connection);
            connected.add(next.id());
        }

        addExtraConnections(selected, unusedPoints, connections, random);

        return new GeneratedMap(
                selected.stream().map(TileDefinition::id).toList(),
                connections
        );
    }

    private void addExtraConnections(
            List<TileDefinition> selected,
            Map<TileId, ArrayDeque<ConnectionPointDefinition>> unusedPoints,
            List<GeneratedConnection> connections,
            RandomGenerator random
    ) {
        ArrayList<TileDefinition> shuffled = new ArrayList<>(selected);
        shuffle(shuffled, random);

        Set<String> existingPairs = new HashSet<>();
        for (GeneratedConnection connection : connections) {
            existingPairs.add(pairKey(
                    connection.first().tileId(),
                    connection.second().tileId()
            ));
        }

        for (TileDefinition first : shuffled) {
            if (unusedPoints.get(first.id()).isEmpty()) {
                continue;
            }

            ArrayList<TileDefinition> partners = new ArrayList<>(shuffled);
            shuffle(partners, random);

            for (TileDefinition second : partners) {
                if (first.id().equals(second.id())) {
                    continue;
                }
                if (unusedPoints.get(second.id()).isEmpty()) {
                    continue;
                }

                String key = pairKey(first.id(), second.id());
                if (existingPairs.contains(key)) {
                    continue;
                }

                connections.add(connect(first.id(), second.id(), unusedPoints));
                existingPairs.add(key);
                break;
            }
        }
    }

    private GeneratedConnection connect(
            TileId first,
            TileId second,
            Map<TileId, ArrayDeque<ConnectionPointDefinition>> unusedPoints
    ) {
        ConnectionPointDefinition firstPoint = unusedPoints.get(first).removeFirst();
        ConnectionPointDefinition secondPoint = unusedPoints.get(second).removeFirst();

        return new GeneratedConnection(
                new TileConnectionPointRef(first, firstPoint.id()),
                new TileConnectionPointRef(second, secondPoint.id())
        );
    }

    private boolean satisfies(
            GeneratedMap generated,
            List<TileDefinition> pool,
            MapGenerationConstraints constraints
    ) {
        if (!generated.isConnected()) {
            return false;
        }

        if (generated.deadEndCount() > constraints.maxDeadEnds()) {
            return false;
        }

        List<TileId> coreIds = pool.stream()
                .filter(tile -> tile.category() == TileCategory.CORE)
                .map(TileDefinition::id)
                .toList();

        if (!generated.tileIds().containsAll(coreIds)) {
            return false;
        }

        for (int i = 0; i < coreIds.size(); i++) {
            for (int j = i + 1; j < coreIds.size(); j++) {
                if (generated.distance(coreIds.get(i), coreIds.get(j))
                        > constraints.maxCoreDistance()) {
                    return false;
                }
            }
        }

        return true;
    }

    private void validatePool(
            List<TileDefinition> pool,
            MapGenerationConstraints constraints
    ) {
        if (pool.size() < constraints.minTiles()) {
            throw new MapGenerationException("Tile pool is smaller than minTiles");
        }

        long distinctIds = pool.stream().map(TileDefinition::id).distinct().count();
        if (distinctIds != pool.size()) {
            throw new MapGenerationException("Tile pool contains duplicate tile ids");
        }

        long coreCount = pool.stream()
                .filter(tile -> tile.category() == TileCategory.CORE)
                .count();

        if (coreCount == 0) {
            throw new MapGenerationException("Tile pool must contain at least one CORE tile");
        }

        if (coreCount > constraints.maxTiles()) {
            throw new MapGenerationException("CORE tile count exceeds maxTiles");
        }
    }

    private static String pairKey(TileId first, TileId second) {
        String a = first.value();
        String b = second.value();
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private static <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
