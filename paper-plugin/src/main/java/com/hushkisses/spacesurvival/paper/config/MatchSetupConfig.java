package com.hushkisses.spacesurvival.paper.config;

public record MatchSetupConfig(
        int mapMinTiles,
        int mapMaxTiles,
        int mapMaxDeadEnds,
        int mapMaxCoreDistance,
        int mapMaxAttempts,
        int secretMissionChancePercent,
        int initialSmallEventsMin,
        int initialSmallEventsMax,
        int accidentWeight,
        int sabotageWeight,
        int infectionWeight
) {
    public MatchSetupConfig {
        if (mapMinTiles < 2 || mapMaxTiles < mapMinTiles) {
            throw new IllegalArgumentException("invalid map tile range");
        }
        if (mapMaxDeadEnds < 0 || mapMaxCoreDistance < 1 || mapMaxAttempts < 1) {
            throw new IllegalArgumentException("invalid map generation constraints");
        }
        if (secretMissionChancePercent < 0 || secretMissionChancePercent > 100) {
            throw new IllegalArgumentException("secretMissionChancePercent");
        }
        if (initialSmallEventsMin < 0 || initialSmallEventsMax < initialSmallEventsMin) {
            throw new IllegalArgumentException("invalid initialSmallEvents range");
        }
        if (accidentWeight < 0 || sabotageWeight < 0 || infectionWeight < 0
                || accidentWeight + sabotageWeight + infectionWeight < 1) {
            throw new IllegalArgumentException("invalid scenario weights");
        }
    }
}
