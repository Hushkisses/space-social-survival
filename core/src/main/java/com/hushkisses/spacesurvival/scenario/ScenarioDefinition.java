package com.hushkisses.spacesurvival.scenario;

import java.util.Objects;

public record ScenarioDefinition(
        ScenarioType type,
        String displayName,
        String publicBriefing,
        String hiddenTruth,
        int minInitialHostiles,
        int maxInitialHostiles,
        boolean hostilesKnowEachOther
) {
    public ScenarioDefinition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(publicBriefing, "publicBriefing");
        Objects.requireNonNull(hiddenTruth, "hiddenTruth");
        if (displayName.isBlank() || publicBriefing.isBlank() || hiddenTruth.isBlank()) {
            throw new IllegalArgumentException("Scenario text must not be blank");
        }
        if (minInitialHostiles < 0 || maxInitialHostiles < minInitialHostiles) {
            throw new IllegalArgumentException("Invalid hostile range");
        }
    }
}
