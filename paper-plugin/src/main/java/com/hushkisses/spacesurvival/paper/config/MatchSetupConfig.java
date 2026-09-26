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
        int infectionWeight,
        int incidentSmallMinSeconds,
        int incidentSmallMaxSeconds,
        int incidentMajorFirstMinSeconds,
        int incidentMajorFirstMaxSeconds,
        int incidentMajorSecondMinSeconds,
        int incidentMajorSecondMaxSeconds
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
        validateRange(incidentSmallMinSeconds, incidentSmallMaxSeconds, "small incident");
        validateRange(
                incidentMajorFirstMinSeconds,
                incidentMajorFirstMaxSeconds,
                "first major incident"
        );
        validateRange(
                incidentMajorSecondMinSeconds,
                incidentMajorSecondMaxSeconds,
                "second major incident"
        );
        if (incidentMajorSecondMinSeconds <= incidentMajorFirstMinSeconds) {
            throw new IllegalArgumentException("second major incident must occur after first");
        }
    }

    private static void validateRange(int min, int max, String name) {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("invalid " + name + " range");
        }
    }
}
