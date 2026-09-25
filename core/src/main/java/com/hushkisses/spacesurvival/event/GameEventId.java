package com.hushkisses.spacesurvival.event;

import java.util.Objects;

public record GameEventId(String value) {
    public GameEventId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("GameEventId");
    }

    @Override
    public String toString() { return value; }
}
