package com.hushkisses.spacesurvival.map;

import java.util.Objects;

public record ConnectionId(String value) {

    public ConnectionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ConnectionId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
