package com.hushkisses.spacesurvival.map;

import java.util.Objects;

public record RoomId(String value) {

    public RoomId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RoomId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
