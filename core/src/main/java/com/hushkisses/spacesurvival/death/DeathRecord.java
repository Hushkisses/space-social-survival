package com.hushkisses.spacesurvival.death;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.time.Instant;
import java.util.Objects;

public record DeathRecord(
        PlayerId playerId,
        DeathCause cause,
        Instant diedAt,
        boolean infectedAtDeath
) {
    public DeathRecord {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(diedAt, "diedAt");
    }
}
