package com.hushkisses.spacesurvival.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record MatchTimeSnapshot(
        Instant startedAt,
        Instant capturedAt,
        Duration elapsed,
        Duration targetDuration,
        boolean running
) {

    public MatchTimeSnapshot {
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(targetDuration, "targetDuration");

        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("elapsed must not be negative");
        }
        if (targetDuration.isZero() || targetDuration.isNegative()) {
            throw new IllegalArgumentException("targetDuration must be positive");
        }
    }

    public Duration remainingToTarget() {
        Duration remaining = targetDuration.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    public boolean targetReached() {
        return elapsed.compareTo(targetDuration) >= 0;
    }
}
