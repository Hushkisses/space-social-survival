package com.hushkisses.spacesurvival.scenario;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;
import java.util.Set;

public record ScenarioRuntime(
        ScenarioDefinition definition,
        Set<PlayerId> initialHostiles
) {
    public ScenarioRuntime {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(initialHostiles, "initialHostiles");
        initialHostiles = Set.copyOf(initialHostiles);
    }

    public boolean isHostile(PlayerId playerId) {
        return initialHostiles.contains(playerId);
    }
}
