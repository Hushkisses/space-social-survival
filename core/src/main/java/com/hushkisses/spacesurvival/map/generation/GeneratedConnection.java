package com.hushkisses.spacesurvival.map.generation;

import com.hushkisses.spacesurvival.map.connection.TileConnectionPointRef;

import java.util.Objects;

public record GeneratedConnection(
        TileConnectionPointRef first,
        TileConnectionPointRef second
) {

    public GeneratedConnection {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        if (first.equals(second)) {
            throw new IllegalArgumentException("Generated connection endpoints must be different");
        }
        if (first.tileId().equals(second.tileId())) {
            throw new IllegalArgumentException("Generated connection must link two different tiles");
        }
    }
}
