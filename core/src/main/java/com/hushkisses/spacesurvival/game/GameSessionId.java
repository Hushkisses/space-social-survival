package com.hushkisses.spacesurvival.game;

import java.util.Objects;
import java.util.UUID;

public record GameSessionId(UUID value) {

    public GameSessionId {
        Objects.requireNonNull(value, "value");
    }

    public static GameSessionId random() {
        return new GameSessionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
