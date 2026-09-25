package com.hushkisses.spacesurvival.paper.match;

import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.scenario.ScenarioRuntime;

import java.util.List;
import java.util.Objects;

public record MatchSetupSnapshot(
        long seed,
        GeneratedMap generatedMap,
        ScenarioRuntime scenario,
        List<String> conflictAxes,
        int initialEventCount
) {
    public MatchSetupSnapshot {
        Objects.requireNonNull(generatedMap, "generatedMap");
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(conflictAxes, "conflictAxes");
        conflictAxes = List.copyOf(conflictAxes);
    }
}
