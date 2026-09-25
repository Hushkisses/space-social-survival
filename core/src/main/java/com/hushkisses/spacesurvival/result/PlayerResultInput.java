package com.hushkisses.spacesurvival.result;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;

public record PlayerResultInput(
        PlayerId playerId,
        boolean alive,
        boolean baseObjectiveCompleted,
        boolean secretObjectiveCompleted,
        int commonContribution,
        int scenarioBonus
) {
    public PlayerResultInput {
        Objects.requireNonNull(playerId, "playerId");
        if (commonContribution < 0) {
            throw new IllegalArgumentException("commonContribution must not be negative");
        }
    }
}
