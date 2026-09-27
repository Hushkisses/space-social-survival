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
            "junction_1",
            "junction_2"
    );

    private static final List<String> OPTIONAL = List.of(
            "auxiliary_1",
            "auxiliary_2",
            "auxiliary_3",
            "auxiliary_4",
            "airlock_1",
            "airlock_2"
    );

    private static final Set<String> UPPER = Set.of(
            "bridge",
            "medical",
            "research",
            "habitation",
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
                throw new MapGenerationException("Missing compact-ship tile: " + id);
            }
        }

        int minTarget = Math.max(constraints.minTiles(), REQUIRED.size());
        int maxTarget = Math.min(
                Math.min(constraints.maxTiles(), REQUIRED.size() + OPTIONAL.size()),
                12
        );
        if (maxTarget < minTarget) {
            throw new MapGenerationException("Compact ship target range is impossible");
        }

        int target = random.nextInt(minTarget, maxTarget + 1);

        LinkedHashSet<String> selected = new LinkedHashSet<>(REQUIRED);
        ArrayList<String> optional = new ArrayList<>(OPTIONAL);
        shuffle(optional, random);

        for (String id : optional) {
            if (selected.size() >= target) break;
            if (byId.containsKey(id)) {
                selected.add(id);
            }
        }

        LinkedHashMap<String, ArrayDeque<ConnectionPointDefinition>> points =
                new LinkedHashMap<>();
        for (String id : selected) {
            points.put(id, new ArrayDeque<>(byId.get(id).connectionPoints()));
        }

        ArrayList<GeneratedConnection> connections = new ArrayList<>();
        HashSet<String> pairs = new HashSet<>();

        // Upper deck compact loop around the command atrium.
        connect("bridge", "junction_1", points, connections, pairs);
        connect("junction_1", "habitation", points, connections, pairs);
        connect("habitation", "medical", points, connections, pairs);
        connect("medical", "research", points, connections, pairs);
        connect("research", "junction_1", points, connections, pairs);

        // One vertical spine only.
        connect("junction_1", "junction_2", points, connections, pairs);

        // Lower deck compact triangle.
        connect("junction_2", "cargo", points, connections, pairs);
        connect("junction_2", "engineering", points, connections, pairs);
        connect("cargo", "engineering", points, connections, pairs);

        ArrayList<String> extras = new ArrayList<>(selected);
        extras.removeAll(REQUIRED);
        shuffle(extras, random);

        for (String extra : extras) {
            boolean upper = upperDeck(extra);
            List<String> preferred = preferredParents(extra);
            String parent = preferred.stream()
                    .filter(selected::contains)
                    .filter(id -> upperDeck(id) == upper)
                    .filter(id -> !points.get(id).isEmpty())
                    .filter(id -> !pairs.contains(pairKey(id, extra)))
                    .findFirst()
                    .orElseGet(() -> selected.stream()
                            .filter(id -> !id.equals(extra))
                            .filter(id -> upperDeck(id) == upper)
                            .filter(id -> !points.get(id).isEmpty())
                            .filter(id -> !pairs.contains(pairKey(id, extra)))
                            .findFirst()
                            .orElseThrow(() -> new MapGenerationException(
                                    "No compact parent available for " + extra
                            )));

            connect(extra, parent, points, connections, pairs);
        }

        GeneratedMap generated = new GeneratedMap(
                selected.stream().map(TileId::new).toList(),
                connections
        );

        if (!generated.isConnected()) {
            throw new MapGenerationException("Compact ship map must be connected");
        }
        if (generated.deadEndCount() > constraints.maxDeadEnds()) {
            throw new MapGenerationException(
                    "Compact ship exceeded dead-end limit: " + generated.deadEndCount()
            );
        }

        return generated;
    }

    private static List<String> preferredParents(String id) {
        return switch (id) {
            case "auxiliary_1" -> List.of("habitation", "junction_1");
            case "auxiliary_2" -> List.of("research", "medical");
            case "airlock_1" -> List.of("research", "junction_1");
            case "auxiliary_3" -> List.of("cargo", "junction_2");
            case "auxiliary_4" -> List.of("engineering", "junction_2");
            case "airlock_2" -> List.of("engineering", "cargo");
            default -> List.of("junction_1", "junction_2");
        };
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
