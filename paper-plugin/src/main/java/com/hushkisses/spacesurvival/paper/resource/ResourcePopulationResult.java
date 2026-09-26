package com.hushkisses.spacesurvival.paper.resource;

public record ResourcePopulationResult(
        int caches,
        int stacks,
        int units
) {
    public ResourcePopulationResult {
        if (caches < 0 || stacks < 0 || units < 0) {
            throw new IllegalArgumentException("negative resource population count");
        }
    }
}
