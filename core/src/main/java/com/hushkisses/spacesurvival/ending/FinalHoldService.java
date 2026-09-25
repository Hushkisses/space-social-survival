package com.hushkisses.spacesurvival.ending;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class FinalHoldService {

    private final Duration required;
    private final Clock clock;

    private FinalHoldStatus status = FinalHoldStatus.NOT_STARTED;
    private Instant startedAt;

    public FinalHoldService(Duration required) {
        this(required, Clock.systemUTC());
    }

    public FinalHoldService(Duration required, Clock clock) {
        this.required = Objects.requireNonNull(required, "required");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (required.isZero() || required.isNegative()) {
            throw new IllegalArgumentException("required must be positive");
        }
    }

    public void start() {
        if (status != FinalHoldStatus.NOT_STARTED) {
            throw new IllegalStateException("Final hold already started");
        }
        startedAt = clock.instant();
        status = FinalHoldStatus.RUNNING;
    }

    public boolean refresh() {
        if (status != FinalHoldStatus.RUNNING) {
            return status == FinalHoldStatus.COMPLETED;
        }
        if (!clock.instant().isBefore(startedAt.plus(required))) {
            status = FinalHoldStatus.COMPLETED;
            return true;
        }
        return false;
    }

    public void fail() {
        if (status == FinalHoldStatus.COMPLETED) {
            throw new IllegalStateException("Completed final hold cannot fail");
        }
        status = FinalHoldStatus.FAILED;
    }

    public FinalHoldSnapshot snapshot() {
        Duration elapsed = startedAt == null
                ? Duration.ZERO
                : Duration.between(startedAt, clock.instant()).isNegative()
                ? Duration.ZERO
                : Duration.between(startedAt, clock.instant());

        if (elapsed.compareTo(required) > 0) {
            elapsed = required;
        }

        Duration remaining = required.minus(elapsed);
        if (remaining.isNegative()) {
            remaining = Duration.ZERO;
        }

        return new FinalHoldSnapshot(status, elapsed, required, remaining);
    }

    public FinalHoldStatus status() {
        return status;
    }
}
