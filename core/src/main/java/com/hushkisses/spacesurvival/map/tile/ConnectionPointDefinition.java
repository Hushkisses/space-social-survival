package com.hushkisses.spacesurvival.map.tile;

import java.util.Objects;

public record ConnectionPointDefinition(
        ConnectionPointId id,
        ConnectionPointType type
) {

    public ConnectionPointDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
    }
}
