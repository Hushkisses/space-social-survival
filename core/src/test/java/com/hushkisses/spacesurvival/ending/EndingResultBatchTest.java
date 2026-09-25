package com.hushkisses.spacesurvival.ending;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.result.*;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import com.hushkisses.spacesurvival.ship.ShipState;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EndingResultBatchTest {

    @Test
    void returnFlowRequiresHealthySystemsAndFacilities() {
        ShipState ship = ShipState.healthy();
        FacilityRegistry facilities = DefaultFacilityCatalog.createRegistry();
        ReturnObjectiveService service = new ReturnObjectiveService(
                ship,
                facilities,
                ReturnRequirements.developmentDefaults()
        );

        ship.set(ShipMetric.POWER, 10);
        assertFalse(service.advanceSurvivalSystems());

        ship.set(ShipMetric.POWER, 100);
        facilities.require(DefaultFacilityCatalog.ENGINEERING)
                .setStatus(FacilityStatus.OFFLINE);
        assertFalse(service.advanceSurvivalSystems());

        facilities.require(DefaultFacilityCatalog.ENGINEERING)
                .setStatus(FacilityStatus.NORMAL);
        assertTrue(service.advanceSurvivalSystems());
        assertEquals(ReturnStage.NAVIGATION, service.stage());

        assertTrue(service.markNavigationReady());
        assertEquals(ReturnStage.RETURN_PREPARATION, service.stage());

        assertTrue(service.markReturnPreparationReady());
        assertEquals(ReturnStage.FINAL_HOLD, service.stage());

        assertTrue(service.complete());
        assertEquals(ReturnStage.COMPLETED, service.stage());
    }

    @Test
    void finalHoldCompletesOnlyAfterRequiredTime() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-25T00:00:00Z"));
        FinalHoldService hold = new FinalHoldService(Duration.ofMinutes(4), clock);

        hold.start();
        clock.advance(Duration.ofMinutes(3).plusSeconds(59));
        assertFalse(hold.refresh());
        assertEquals(1, hold.snapshot().remaining().toSeconds());

        clock.advance(Duration.ofSeconds(1));
        assertTrue(hold.refresh());
        assertEquals(FinalHoldStatus.COMPLETED, hold.status());
        assertEquals(0, hold.snapshot().remaining().toSeconds());
    }

    @Test
    void commonMissionFailureProducesNoWinners() {
        PlayerId player = PlayerId.of(UUID.randomUUID());
        ResultEvaluator evaluator = new ResultEvaluator(
                ResultScoringConfig.developmentDefaults()
        );

        MatchResult result = evaluator.evaluate(
                List.of(new PlayerResultInput(player, true, true, true, 3, 0)),
                false
        );

        assertTrue(result.winners().isEmpty());
        assertTrue(result.mvps().isEmpty());
    }

    @Test
    void baseObjectiveQualifiesWinnerAndMvpAllowsTies() {
        PlayerId first = PlayerId.of(UUID.randomUUID());
        PlayerId second = PlayerId.of(UUID.randomUUID());
        PlayerId third = PlayerId.of(UUID.randomUUID());

        ResultEvaluator evaluator = new ResultEvaluator(
                ResultScoringConfig.developmentDefaults()
        );

        MatchResult result = evaluator.evaluate(
                List.of(
                        new PlayerResultInput(first, true, true, true, 3, 0),
                        new PlayerResultInput(second, true, true, true, 3, 0),
                        new PlayerResultInput(third, true, false, true, 3, 20)
                ),
                true
        );

        assertEquals(Set.of(first, second), result.winners());
        assertEquals(Set.of(first, second), result.mvps());
        assertFalse(result.winners().contains(third));
    }

    @Test
    void survivalIsBonusNotWinnerRequirement() {
        PlayerId player = PlayerId.of(UUID.randomUUID());
        ResultEvaluator evaluator = new ResultEvaluator(
                ResultScoringConfig.developmentDefaults()
        );

        MatchResult result = evaluator.evaluate(
                List.of(new PlayerResultInput(player, false, true, false, 1, 0)),
                true
        );

        assertTrue(result.winners().contains(player));
        assertEquals(6, result.players().getFirst().score().total());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
