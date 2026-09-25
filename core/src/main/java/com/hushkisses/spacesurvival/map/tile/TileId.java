package com.hushkisses.spacesurvival.map.tile;

import java.util.Objects;

public record TileId(String value) {

    public TileId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("TileId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
