package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.*;

public final class GeneratedMap {

    private final Set<TileId> tileIds;
    private final List<GeneratedConnection> connections;
    private final Map<TileId, Set<TileId>> adjacency;

    public GeneratedMap(
            Collection<TileId> tileIds,
            Collection<GeneratedConnection> connections
    ) {
        Objects.requireNonNull(tileIds, "tileIds");
        Objects.requireNonNull(connections, "connections");

        LinkedHashSet<TileId> tiles = new LinkedHashSet<>(tileIds);
        if (tiles.size() != tileIds.size()) {
            throw new IllegalArgumentException("Generated map contains duplicate tile ids");
        }
        if (tiles.isEmpty()) {
            throw new IllegalArgumentException("Generated map must contain at least one tile");
        }

        LinkedHashMap<TileId, Set<TileId>> graph = new LinkedHashMap<>();
        for (TileId tileId : tiles) {
            graph.put(tileId, new LinkedHashSet<>());
        }

        ArrayList<GeneratedConnection> connectionList = new ArrayList<>();
        for (GeneratedConnection connection : connections) {
            if (!tiles.contains(connection.first().tileId())
                    || !tiles.contains(connection.second().tileId())) {
                throw new IllegalArgumentException("Connection references a tile outside generated map");
            }

            graph.get(connection.first().tileId()).add(connection.second().tileId());
            graph.get(connection.second().tileId()).add(connection.first().tileId());
            connectionList.add(connection);
        }

        this.tileIds = Collections.unmodifiableSet(tiles);
        this.connections = Collections.unmodifiableList(connectionList);

        LinkedHashMap<TileId, Set<TileId>> immutableGraph = new LinkedHashMap<>();
        graph.forEach((key, value) ->
                immutableGraph.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(value))));
        this.adjacency = Collections.unmodifiableMap(immutableGraph);
    }

    public Set<TileId> tileIds() {
        return tileIds;
    }

    public List<GeneratedConnection> connections() {
        return connections;
    }

    public Set<TileId> adjacent(TileId tileId) {
        Set<TileId> result = adjacency.get(Objects.requireNonNull(tileId, "tileId"));
        if (result == null) {
            throw new IllegalArgumentException("Unknown generated tile: " + tileId);
        }
        return result;
    }

    public int degree(TileId tileId) {
        return adjacent(tileId).size();
    }

    public boolean isConnected() {
        if (tileIds.size() == 1) {
            return true;
        }

        Set<TileId> visited = new HashSet<>();
        ArrayDeque<TileId> queue = new ArrayDeque<>();
        TileId start = tileIds.iterator().next();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            TileId current = queue.removeFirst();
            for (TileId next : adjacency.get(current)) {
                if (visited.add(next)) {
                    queue.addLast(next);
                }
            }
        }

        return visited.size() == tileIds.size();
    }

    public int distance(TileId from, TileId to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        if (!tileIds.contains(from) || !tileIds.contains(to)) {
            throw new IllegalArgumentException("Both tiles must belong to generated map");
        }
        if (from.equals(to)) {
            return 0;
        }

        ArrayDeque<TileId> queue = new ArrayDeque<>();
        Map<TileId, Integer> distances = new HashMap<>();
        queue.add(from);
        distances.put(from, 0);

        while (!queue.isEmpty()) {
            TileId current = queue.removeFirst();
            int nextDistance = distances.get(current) + 1;

            for (TileId next : adjacency.get(current)) {
                if (distances.containsKey(next)) {
                    continue;
                }
                if (next.equals(to)) {
                    return nextDistance;
                }
                distances.put(next, nextDistance);
                queue.addLast(next);
            }
        }

        return Integer.MAX_VALUE;
    }

    public long deadEndCount() {
        return tileIds.stream()
                .filter(tileId -> degree(tileId) == 1)
                .count();
    }
}
