package com.hushkisses.spacesurvival.map.connection;

import java.util.Objects;

public record ConnectionRoute(
        TileConnectionPointRef first,
        TileConnectionPointRef second,
        ConnectionState state
) {

    public ConnectionRoute {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        Objects.requireNonNull(state, "state");

        if (first.equals(second)) {
            throw new IllegalArgumentException("Connection route endpoints must be different");
        }
    }

    public TileConnectionPointRef other(TileConnectionPointRef endpoint) {
        Objects.requireNonNull(endpoint, "endpoint");

        if (first.equals(endpoint)) {
            return second;
        }
        if (second.equals(endpoint)) {
            return first;
        }

        throw new IllegalArgumentException("Endpoint is not part of this route: " + endpoint);
    }
}
