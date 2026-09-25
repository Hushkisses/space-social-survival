package com.hushkisses.spacesurvival.time;

import java.time.Duration;
import java.util.Objects;

public final class CrisisEvaluator {

    private final CrisisThresholds thresholds;

    public CrisisEvaluator(CrisisThresholds thresholds) {
        this.thresholds = Objects.requireNonNull(thresholds, "thresholds");
    }

    public CrisisStage evaluate(Duration elapsed, CrisisFactors factors) {
        Objects.requireNonNull(elapsed, "elapsed");
        Objects.requireNonNull(factors, "factors");

        if (elapsed.isNegative()) {
            throw new IllegalArgumentException("elapsed must not be negative");
        }

        long elapsedMinutes = elapsed.toMinutes();
        long score = elapsedMinutes + factors.externalPressure();

        if (score >= thresholds.collapseScore()) {
            return CrisisStage.COLLAPSE;
        }
        if (score >= thresholds.crisisScore()) {
            return CrisisStage.CRISIS;
        }
        if (score >= thresholds.alertScore()) {
            return CrisisStage.ALERT;
        }
        return CrisisStage.STABLE;
    }
}
