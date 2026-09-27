package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.connection.TileConnectionPointRef;
import com.hushkisses.spacesurvival.map.tile.ConnectionPointDefinition;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;
import java.util.random.RandomGenerator;

public final class CompactSingleDeckMapGenerator {

    private static final List<String> REQUIRED = List.of(
            "bridge",
            "engineering",
            "medical",
            "research",
            "cargo",
            "habitation",
            "junction_1"
    );

    private static final List<String> OPTIONAL = List.of(
            "auxiliary_1",
            "auxiliary_2",
            "auxiliary_3",
            "auxiliary_4",
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
                throw new MapGenerationException("Missing compact single-deck tile: " + id);
            }
        }

        LinkedHashSet<String> selected = new LinkedHashSet<>(REQUIRED);
        ArrayList<String> optional = new ArrayList<>(OPTIONAL);
        shuffle(optional, random);

        // 6-10 players: six unique core facilities + one hub + exactly three
        // small optional rooms. The ship stays at ten meaningful rooms.
        for (String id : optional.subList(0, Math.min(3, optional.size()))) {
            if (byId.containsKey(id)) {
                selected.add(id);
            }
        }

        if (selected.size() != 10) {
            throw new MapGenerationException(
                    "Compact ship requires exactly 10 selected rooms, got " + selected.size()
            );
        }

        LinkedHashMap<String, ArrayDeque<ConnectionPointDefinition>> points =
                new LinkedHashMap<>();
        for (String id : selected) {
            points.put(id, new ArrayDeque<>(byId.get(id).connectionPoints()));
        }

        ArrayList<GeneratedConnection> connections = new ArrayList<>();
        HashSet<String> pairs = new HashSet<>();

        // Central hub: one glance reveals the four main directions.
        connect("bridge", "junction_1", points, connections, pairs);
        connect("junction_1", "habitation", points, connections, pairs);
        connect("junction_1", "medical", points, connections, pairs);
        connect("junction_1", "engineering", points, connections, pairs);

        // Two short side loops keep movement compact without becoming a maze.
        connect("habitation", "cargo", points, connections, pairs);
        connect("cargo", "engineering", points, connections, pairs);
        connect("medical", "research", points, connections, pairs);
        connect("research", "engineering", points, connections, pairs);

        for (String extra : selected) {
            if (REQUIRED.contains(extra)) {
                continue;
            }

            String parent = preferredParent(extra);
            if (!selected.contains(parent)) {
                throw new MapGenerationException(
                        "Preferred compact parent missing for " + extra + ": " + parent
                );
            }
            connect(extra, parent, points, connections, pairs);
        }

        GeneratedMap generated = new GeneratedMap(
                selected.stream().map(TileId::new).toList(),
                connections
        );

        if (!generated.isConnected()) {
            throw new MapGenerationException("Compact single-deck map must be connected");
        }
        if (generated.deadEndCount() > constraints.maxDeadEnds()) {
            throw new MapGenerationException(
                    "Compact single-deck map exceeded dead-end limit: "
                            + generated.deadEndCount()
            );
        }

        return generated;
    }

    private static String preferredParent(String id) {
        return switch (id) {
            case "auxiliary_1" -> "habitation";
            case "auxiliary_2" -> "medical";
            case "auxiliary_3" -> "cargo";
            case "auxiliary_4" -> "research";
            case "airlock_1" -> "engineering";
            default -> throw new MapGenerationException("Unknown optional room: " + id);
        };
    }

    private static void connect(
            String first,
            String second,
            Map<String, ArrayDeque<ConnectionPointDefinition>> points,
            List<GeneratedConnection> connections,
            Set<String> pairs
    ) {
        String key = pairKey(first, second);
        if (!pairs.add(key)) {
            return;
        }

        ArrayDeque<ConnectionPointDefinition> firstPoints = points.get(first);
        ArrayDeque<ConnectionPointDefinition> secondPoints = points.get(second);
        if (firstPoints == null || secondPoints == null) {
            throw new MapGenerationException("Missing endpoint for " + first + " <-> " + second);
        }
        if (firstPoints.isEmpty() || secondPoints.isEmpty()) {
            throw new MapGenerationException(
                    "Connection capacity exhausted for " + first + " <-> " + second
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
