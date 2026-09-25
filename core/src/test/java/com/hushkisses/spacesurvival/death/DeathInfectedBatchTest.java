package com.hushkisses.spacesurvival.death;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.player.PlayerState;
import com.hushkisses.spacesurvival.scenario.DefaultScenarioCatalog;
import com.hushkisses.spacesurvival.scenario.ScenarioRuntime;
import com.hushkisses.spacesurvival.scenario.ScenarioType;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeathInfectedBatchTest {

    @Test
    void deathMarksPlayerDeadAndIsIdempotent() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-25T00:00:00Z"));
        DeathService service = new DeathService(clock);
        PlayerState player = PlayerState.create(
                PlayerId.of(UUID.randomUUID()),
                clock
        );

        DeathRecord first = service.registerDeath(player, DeathCause.ENVIRONMENT, false);
        clock.advance(Duration.ofMinutes(1));
        DeathRecord second = service.registerDeath(player, DeathCause.COMBAT, true);

        assertFalse(player.isAlive());
        assertEquals(first, second);
        assertFalse(first.infectedAtDeath());
    }

    @Test
    void infectedDeathConvertsOnlyInInfectionScenario() {
        PlayerId playerId = PlayerId.of(UUID.randomUUID());
        DeathRecord infectedDeath = new DeathRecord(
                playerId,
                DeathCause.ENVIRONMENT,
                Instant.now(),
                true
        );

        ScenarioRuntime infection = new ScenarioRuntime(
                DefaultScenarioCatalog.create().stream()
                        .filter(definition -> definition.type() == ScenarioType.INFECTION)
                        .findFirst()
                        .orElseThrow(),
                Set.of()
        );

        InfectedPlayerService service = new InfectedPlayerService();
        InfectedPlayerState infected = service.onDeath(infectedDeath, infection);

        assertEquals(PostDeathForm.INFECTED, infected.form());
        assertFalse(infected.canRepair());
        assertFalse(infected.canSpeakToLiving());
        assertTrue(infected.goals().contains(InfectedPostDeathGoal.INFECT_OTHERS));
    }

    @Test
    void uninfectedDeathStaysDead() {
        PlayerId playerId = PlayerId.of(UUID.randomUUID());
        DeathRecord death = new DeathRecord(
                playerId,
                DeathCause.ENVIRONMENT,
                Instant.now(),
                false
        );

        InfectedPlayerState state = new InfectedPlayerService().onDeath(death, null);

        assertEquals(PostDeathForm.DEAD, state.form());
        assertTrue(state.goals().isEmpty());
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
