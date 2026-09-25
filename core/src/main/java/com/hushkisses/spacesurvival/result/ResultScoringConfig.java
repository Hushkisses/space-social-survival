package com.hushkisses.spacesurvival.result;

public record ResultScoringConfig(
        int survivalPoints,
        int baseObjectivePoints,
        int secretObjectivePoints,
        int maxCommonContribution
) {
    public ResultScoringConfig {
        if (survivalPoints < 0
                || baseObjectivePoints < 0
                || secretObjectivePoints < 0
                || maxCommonContribution < 0) {
            throw new IllegalArgumentException("Scoring values must not be negative");
        }
    }

    public static ResultScoringConfig developmentDefaults() {
        return new ResultScoringConfig(3, 5, 3, 3);
    }
}
