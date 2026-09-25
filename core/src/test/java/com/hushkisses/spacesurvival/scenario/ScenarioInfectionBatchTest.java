package com.hushkisses.spacesurvival.scenario;

import com.hushkisses.spacesurvival.infection.*;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ScenarioInfectionBatchTest {

    @Test
    void accidentNeverAssignsInitialHostiles() {
        ScenarioDefinition accident = definition(ScenarioType.ACCIDENT);
        ScenarioRuntime runtime = new ScenarioEngine().activate(
                accident,
                players(6),
                new Random(1)
        );

        assertTrue(runtime.initialHostiles().isEmpty());
    }

    @Test
    void sabotageAssignsZeroToTwoHostilesDeterministically() {
        ScenarioDefinition sabotage = definition(ScenarioType.SABOTAGE);
        ScenarioRuntime first = new ScenarioEngine().activate(
                sabotage,
                players(6),
                new Random(12345)
        );
        ScenarioRuntime second = new ScenarioEngine().activate(
                sabotage,
                players(6),
                new Random(12345)
        );

        assertEquals(first.initialHostiles(), second.initialHostiles());
        assertTrue(first.initialHostiles().size() <= 2);
    }

    @Test
    void infectionTestsLimitPrecisionWithoutLying() {
        PlayerId player = players(1).getFirst();
        InfectionService infections = new InfectionService();

        assertEquals(InfectionTestResult.NEGATIVE, infections.test(player, false));

        infections.expose(player);
        assertEquals(InfectionStage.EXPOSED, infections.state(player).stage());
        assertEquals(InfectionTestResult.INCONCLUSIVE, infections.test(player, false));
        assertEquals(InfectionTestResult.POSITIVE, infections.test(player, true));

        infections.advance(player, 70);
        assertEquals(InfectionStage.SYMPTOMATIC, infections.state(player).stage());
        assertEquals(InfectionTestResult.POSITIVE, infections.test(player, false));

        infections.suppress(player);
        assertEquals(InfectionStage.SUPPRESSED, infections.state(player).stage());
        infections.cure(player);
        assertEquals(InfectionStage.NONE, infections.state(player).stage());
    }

    @Test
    void outbreakOnlyRunsInInfectionScenario() {
        List<PlayerId> players = players(6);
        InfectionService infections = new InfectionService();
        InfectionScenarioService service = new InfectionScenarioService();

        ScenarioRuntime infection = new ScenarioEngine().activate(
                definition(ScenarioType.INFECTION),
                players,
                new Random(4)
        );

        PlayerId selected = service.beginOutbreak(
                infection,
                players,
                infections,
                new Random(9)
        );

        assertTrue(infections.state(selected).infected());

        ScenarioRuntime accident = new ScenarioRuntime(
                definition(ScenarioType.ACCIDENT),
                java.util.Set.of()
        );
        assertThrows(
                IllegalStateException.class,
                () -> service.beginOutbreak(accident, players, infections, new Random(1))
        );
    }

    private static ScenarioDefinition definition(ScenarioType type) {
        return DefaultScenarioCatalog.create().stream()
                .filter(definition -> definition.type() == type)
                .findFirst()
                .orElseThrow();
    }

    private static List<PlayerId> players(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> PlayerId.of(
                        UUID.nameUUIDFromBytes(("scenario-player-" + index).getBytes())
                ))
                .toList();
    }
}
