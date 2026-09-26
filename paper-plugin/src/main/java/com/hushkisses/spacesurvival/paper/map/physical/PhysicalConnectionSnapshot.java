package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.connection.ConnectionState;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;

import java.util.Objects;

public record PhysicalConnectionSnapshot(
        int id,
        GeneratedConnection connection,
        ConnectionState state
) {
    public PhysicalConnectionSnapshot {
        if (id < 0) throw new IllegalArgumentException("id");
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(state, "state");
    }
}
