package com.hushkisses.spacesurvival.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class GameTimer {

    private final Clock clock;
    private final Duration targetDuration;

    private Instant startedAt;
    private Instant stoppedAt;

    public GameTimer(Duration targetDuration) {
        this(targetDuration, Clock.systemUTC());
    }

    public GameTimer(Duration targetDuration, Clock clock) {
        this.targetDuration = Objects.requireNonNull(targetDuration, "targetDuration");
        this.clock = Objects.requireNonNull(clock, "clock");

        if (targetDuration.isZero() || targetDuration.isNegative()) {
            throw new IllegalArgumentException("targetDuration must be positive");
        }
    }

    public void start() {
        if (startedAt != null) {
            throw new IllegalStateException("Game timer has already started");
        }
        startedAt = clock.instant();
    }

    public void stop() {
        ensureStarted();
        if (stoppedAt == null) {
            stoppedAt = clock.instant();
        }
    }

    public boolean isStarted() {
        return startedAt != null;
    }

    public boolean isRunning() {
        return startedAt != null && stoppedAt == null;
    }

    public MatchTimeSnapshot snapshot() {
        ensureStarted();

        Instant capturedAt = stoppedAt == null ? clock.instant() : stoppedAt;
        Duration elapsed = Duration.between(startedAt, capturedAt);

        return new MatchTimeSnapshot(
                startedAt,
                capturedAt,
                elapsed,
                targetDuration,
                stoppedAt == null
        );
    }

    private void ensureStarted() {
        if (startedAt == null) {
            throw new IllegalStateException("Game timer has not started");
        }
    }
}
