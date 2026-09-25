package com.hushkisses.spacesurvival.map.tile;

import java.util.Objects;

public record ConnectionPointId(String value) {

    public ConnectionPointId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ConnectionPointId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
