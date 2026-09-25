package com.hushkisses.spacesurvival.resource.node;

import java.util.Objects;

public record ResourceNodeId(String value) {
    public ResourceNodeId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("ResourceNodeId");
    }

    @Override
    public String toString() { return value; }
}
