package com.hushkisses.spacesurvival.player;

import java.util.Objects;
import java.util.UUID;

public record PlayerId(UUID value) {

    public PlayerId {
        Objects.requireNonNull(value, "value");
    }

    public static PlayerId of(UUID value) {
        return new PlayerId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
