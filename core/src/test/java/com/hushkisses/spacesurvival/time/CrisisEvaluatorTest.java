package com.hushkisses.spacesurvival.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class CrisisEvaluatorTest {

    private final CrisisThresholds thresholds =
            CrisisThresholds.defaultForTargetMinutes(45);
    private final CrisisEvaluator evaluator = new CrisisEvaluator(thresholds);

    @Test
    void defaultThresholdsMatchFortyFiveMinuteBaseline() {
        assertEquals(15, thresholds.alertScore());
        assertEquals(30, thresholds.crisisScore());
        assertEquals(60, thresholds.collapseScore());
    }

    @Test
    void timeRaisesBaselineCrisisStage() {
        assertEquals(
                CrisisStage.STABLE,
                evaluator.evaluate(Duration.ofMinutes(14), CrisisFactors.neutral())
        );
        assertEquals(
                CrisisStage.ALERT,
                evaluator.evaluate(Duration.ofMinutes(15), CrisisFactors.neutral())
        );
        assertEquals(
                CrisisStage.CRISIS,
                evaluator.evaluate(Duration.ofMinutes(30), CrisisFactors.neutral())
        );
        assertEquals(
                CrisisStage.COLLAPSE,
                evaluator.evaluate(Duration.ofMinutes(60), CrisisFactors.neutral())
        );
    }

    @Test
    void positiveExternalPressureAcceleratesCrisis() {
        assertEquals(
                CrisisStage.CRISIS,
                evaluator.evaluate(Duration.ofMinutes(20), new CrisisFactors(10))
        );
    }

    @Test
    void negativeExternalPressureCanDelayCrisis() {
        assertEquals(
                CrisisStage.STABLE,
                evaluator.evaluate(Duration.ofMinutes(20), new CrisisFactors(-10))
        );
    }
}
