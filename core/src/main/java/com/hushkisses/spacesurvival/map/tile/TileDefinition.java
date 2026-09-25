package com.hushkisses.spacesurvival.map.tile;

import java.util.*;

public final class TileDefinition {

    private final TileId id;
    private final TileCategory category;
    private final String displayName;
    private final Map<ConnectionPointId, ConnectionPointDefinition> connectionPoints;

    public TileDefinition(
            TileId id,
            TileCategory category,
            String displayName,
            Collection<ConnectionPointDefinition> connectionPoints
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.category = Objects.requireNonNull(category, "category");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(connectionPoints, "connectionPoints");

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }

        LinkedHashMap<ConnectionPointId, ConnectionPointDefinition> registered = new LinkedHashMap<>();
        for (ConnectionPointDefinition point : connectionPoints) {
            Objects.requireNonNull(point, "connection point");
            if (registered.putIfAbsent(point.id(), point) != null) {
                throw new IllegalArgumentException(
                        "Duplicate connection point id in tile " + id + ": " + point.id()
                );
            }
        }

        this.connectionPoints = Collections.unmodifiableMap(registered);
    }

    public TileId id() {
        return id;
    }

    public TileCategory category() {
        return category;
    }

    public String displayName() {
        return displayName;
    }

    public Collection<ConnectionPointDefinition> connectionPoints() {
        return connectionPoints.values();
    }

    public Optional<ConnectionPointDefinition> connectionPoint(ConnectionPointId id) {
        return Optional.ofNullable(connectionPoints.get(Objects.requireNonNull(id, "id")));
    }
}
