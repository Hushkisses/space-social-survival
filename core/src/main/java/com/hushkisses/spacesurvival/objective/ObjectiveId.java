package com.hushkisses.spacesurvival.objective;

import java.util.Objects;

public record ObjectiveId(String value) {
    public ObjectiveId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("ObjectiveId");
    }

    @Override
    public String toString() { return value; }
}
