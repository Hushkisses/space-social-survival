package com.hushkisses.spacesurvival.resource.processing;

import java.util.Objects;

public record ProcessingRecipeId(String value) {
    public ProcessingRecipeId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("ProcessingRecipeId");
    }

    @Override
    public String toString() { return value; }
}
