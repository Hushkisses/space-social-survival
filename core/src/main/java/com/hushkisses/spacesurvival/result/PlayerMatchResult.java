package com.hushkisses.spacesurvival.result;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;

public record PlayerMatchResult(
        PlayerId playerId,
        boolean winner,
        ScoreBreakdown score
) {
    public PlayerMatchResult {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(score, "score");
    }
}
