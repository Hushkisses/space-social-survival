package com.hushkisses.spacesurvival.role;

import java.util.Objects;

public record RoleId(String value) {

    public RoleId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RoleId must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
