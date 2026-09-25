package com.hushkisses.spacesurvival.ending;

import java.time.Duration;

public record FinalHoldSnapshot(
        FinalHoldStatus status,
        Duration elapsed,
        Duration required,
        Duration remaining
) {
}
