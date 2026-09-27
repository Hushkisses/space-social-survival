package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.connection.TileConnectionPointRef;
import com.hushkisses.spacesurvival.map.tile.ConnectionPointDefinition;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;
import java.util.random.RandomGenerator;

public final class CohesiveDeckMapGenerator {

    private static final List<String> REQUIRED = List.of(
            "bridge",
            "engineering",
            "medical",
            "research",
            "cargo",
            "habitation",
            "corridor_1",
            "corridor_2",
            "junction_1",
            "junction_2"
    );

    private static final Set<String> UPPER = Set.of(
            "bridge",
            "medical",
            "research",
            "habitation",
            "corridor_1",
            "corridor_4",
            "corridor_5",
            "junction_1",
            "auxiliary_1",
            "auxiliary_2",
            "airlock_1"
    );

    public GeneratedMap generate(
            Collection<TileDefinition> candidates,
            MapGenerationConstraints constraints,
            RandomGenerator random
    ) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(constraints, "constraints");
        Objects.requireNonNull(random, "random");

        Map<String, TileDefinition> byId = new LinkedHashMap<>();
        for (TileDefinition definition : candidates) {
            byId.put(definition.id().value(), definition);
        }

        for (String id : REQUIRED) {
            if (!byId.containsKey(id)) {
                throw new MapGenerationException("Missing required cohesive-deck tile: " + id);
            }
        }

        int minTarget = Math.max(constraints.minTiles(), REQUIRED.size());
        int maxTarget = Math.min(constraints.maxTiles(), byId.size());
        if (maxTarget < minTarget) {
            throw new MapGenerationException("Cohesive-deck target range is impossible");
        }

        int target = random.nextInt(minTarget, maxTarget + 1);

        LinkedHashSet<String> selectedIds = new LinkedHashSet<>(REQUIRED);
        ArrayList<String> optional = new ArrayList<>(byId.keySet());
        optional.removeAll(selectedIds);
        shuffle(optional, random);

        for (String id : optional) {
            if (selectedIds.size() >= target) break;
            selectedIds.add(id);
        }

        LinkedHashMap<String, ArrayDeque<ConnectionPointDefinition>> points =
                new LinkedHashMap<>();
        for (String id : selectedIds) {
            points.put(
                    id,
                    new ArrayDeque<>(byId.get(id).connectionPoints())
            );
        }

        ArrayList<GeneratedConnection> connections = new ArrayList<>();
        HashSet<String> pairs = new HashSet<>();

        connect("bridge", "corridor_1", points, connections, pairs);
        connect("corridor_1", "junction_1", points, connections, pairs);
        connect("junction_1", "habitation", points, connections, pairs);
        connect("junction_1", "medical", points, connections, pairs);
        connect("medical", "research", points, connections, pairs);

        // The only mandatory inter-deck link. Physically this becomes the central stairwell.
        connect("junction_1", "junction_2", points, connections, pairs);

        connect("junction_2", "cargo", points, connections, pairs);
        connect("cargo", "corridor_2", points, connections, pairs);
        connect("corridor_2", "engineering", points, connections, pairs);

        ArrayList<String> extras = new ArrayList<>(selectedIds);
        extras.removeAll(REQUIRED);
        shuffle(extras, random);

        for (String extra : extras) {
            boolean upper = upperDeck(extra);
            List<String> parents = selectedIds.stream()
                    .filter(id -> !id.equals(extra))
                    .filter(id -> upperDeck(id) == upper)
                    .filter(id -> !points.get(id).isEmpty())
                    .filter(id -> !pairs.contains(pairKey(id, extra)))
                    .toList();

            if (parents.isEmpty() || points.get(extra).isEmpty()) {
                throw new MapGenerationException("No capacity to attach optional tile: " + extra);
            }

            ArrayList<String> shuffledParents = new ArrayList<>(parents);
            shuffle(shuffledParents, random);
            connect(extra, shuffledParents.getFirst(), points, connections, pairs);

            if (!extra.startsWith("airlock_") && !points.get(extra).isEmpty()) {
                List<String> secondParents = shuffledParents.stream()
                        .filter(id -> !points.get(id).isEmpty())
                        .filter(id -> !pairs.contains(pairKey(id, extra)))
                        .toList();
                if (!secondParents.isEmpty()) {
                    connect(extra, secondParents.getFirst(), points, connections, pairs);
                }
            }
        }

        GeneratedMap generated = new GeneratedMap(
                selectedIds.stream().map(TileId::new).toList(),
                connections
        );

        if (!generated.isConnected()) {
            throw new MapGenerationException("Cohesive-deck map must be connected");
        }
        if (generated.deadEndCount() > constraints.maxDeadEnds()) {
            throw new MapGenerationException(
                    "Cohesive-deck map exceeded dead-end limit: " + generated.deadEndCount()
            );
        }

        List<TileId> core = List.of(
                new TileId("bridge"),
                new TileId("engineering"),
                new TileId("medical"),
                new TileId("research"),
                new TileId("cargo"),
                new TileId("habitation")
        );
        for (int i = 0; i < core.size(); i++) {
            for (int j = i + 1; j < core.size(); j++) {
                if (generated.distance(core.get(i), core.get(j)) > constraints.maxCoreDistance()) {
                    throw new MapGenerationException("Core distance exceeded cohesive-deck limit");
                }
            }
        }

        return generated;
    }

    private static boolean upperDeck(String id) {
        return UPPER.contains(id);
    }

    private static void connect(
            String first,
            String second,
            Map<String, ArrayDeque<ConnectionPointDefinition>> points,
            List<GeneratedConnection> connections,
            Set<String> pairs
    ) {
        if (!points.containsKey(first) || !points.containsKey(second)) {
            return;
        }

        String key = pairKey(first, second);
        if (!pairs.add(key)) {
            return;
        }

        ArrayDeque<ConnectionPointDefinition> firstPoints = points.get(first);
        ArrayDeque<ConnectionPointDefinition> secondPoints = points.get(second);
        if (firstPoints.isEmpty() || secondPoints.isEmpty()) {
            throw new MapGenerationException(
                    "Connection point capacity exhausted for " + first + " <-> " + second
            );
        }

        connections.add(new GeneratedConnection(
                new TileConnectionPointRef(
                        new TileId(first),
                        firstPoints.removeFirst().id()
                ),
                new TileConnectionPointRef(
                        new TileId(second),
                        secondPoints.removeFirst().id()
                )
        ));
    }

    private static String pairKey(String first, String second) {
        return first.compareTo(second) <= 0
                ? first + "|" + second
                : second + "|" + first;
    }

    private static <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }
}
