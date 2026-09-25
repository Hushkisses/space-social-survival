package com.hushkisses.spacesurvival.event;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

public final class GameEventSelector {

    public GameEventDefinition select(
            GameEventRegistry registry,
            GameEventScale scale,
            RandomGenerator random
    ) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(scale, "scale");
        Objects.requireNonNull(random, "random");

        List<GameEventDefinition> candidates = registry.byScale(scale);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("No events registered for scale: " + scale);
        }

        return candidates.get(random.nextInt(candidates.size()));
    }
}
