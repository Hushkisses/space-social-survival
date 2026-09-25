package com.hushkisses.spacesurvival.map.generation;

public record MapGenerationConstraints(
        int minTiles,
        int maxTiles,
        int maxDeadEnds,
        int maxCoreDistance,
        int maxAttempts
) {

    public MapGenerationConstraints {
        if (minTiles < 2) {
            throw new IllegalArgumentException("minTiles must be at least 2");
        }
        if (maxTiles < minTiles) {
            throw new IllegalArgumentException("maxTiles must be >= minTiles");
        }
        if (maxDeadEnds < 0) {
            throw new IllegalArgumentException("maxDeadEnds must be >= 0");
        }
        if (maxCoreDistance < 1) {
            throw new IllegalArgumentException("maxCoreDistance must be >= 1");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
    }
}
