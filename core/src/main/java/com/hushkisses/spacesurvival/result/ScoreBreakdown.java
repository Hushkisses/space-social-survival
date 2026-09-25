package com.hushkisses.spacesurvival.result;

public record ScoreBreakdown(
        int survival,
        int baseObjective,
        int secretObjective,
        int commonContribution,
        int scenarioBonus
) {
    public int total() {
        return survival + baseObjective + secretObjective + commonContribution + scenarioBonus;
    }
}
