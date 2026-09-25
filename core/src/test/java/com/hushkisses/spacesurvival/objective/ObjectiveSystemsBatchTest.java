package com.hushkisses.spacesurvival.objective;

import com.hushkisses.spacesurvival.objective.assignment.ObjectiveAssignmentService;
import com.hushkisses.spacesurvival.objective.conflict.ConflictSetDefinition;
import com.hushkisses.spacesurvival.objective.conflict.ConflictSetSelector;
import com.hushkisses.spacesurvival.objective.conflict.DefaultConflictSetCatalog;
import com.hushkisses.spacesurvival.objective.secret.SecretMissionGrantResult;
import com.hushkisses.spacesurvival.objective.secret.SecretMissionService;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ObjectiveSystemsBatchTest {

    @Test
    void initialCatalogContainsTwentyFourObjectives() {
        assertEquals(24, DefaultObjectiveCatalog.createRegistry().size());
    }

    @Test
    void conflictSelectorChoosesTwoOrThreeAxesDeterministically() {
        ObjectiveRegistry registry = DefaultObjectiveCatalog.createRegistry();
        List<ConflictSetDefinition> available = DefaultConflictSetCatalog.create(registry);
        ConflictSetSelector selector = new ConflictSetSelector();

        List<ConflictSetDefinition> first =
                selector.select(available, 2, 3, new Random(77));
        List<ConflictSetDefinition> second =
                selector.select(available, 2, 3, new Random(77));

        assertEquals(first, second);
        assertTrue(first.size() >= 2 && first.size() <= 3);
    }

    @Test
    void baseObjectivesAreAssignedAroundSelectedConflictSets() {
        ObjectiveRegistry registry = DefaultObjectiveCatalog.createRegistry();
        ObjectiveEngine engine = new ObjectiveEngine();
        List<PlayerId> players = players(6);
        List<ConflictSetDefinition> selected = new ConflictSetSelector().select(
                DefaultConflictSetCatalog.create(registry),
                2,
                3,
                new Random(10)
        );

        var assigned = new ObjectiveAssignmentService(registry)
                .assignBaseObjectives(players, selected, engine, new Random(11));

        assertEquals(6, assigned.size());
        for (PlayerId player : players) {
            assertTrue(engine.objective(player, ObjectiveSlot.BASE).isPresent());
            assertTrue(engine.objective(player, ObjectiveSlot.SECRET).isEmpty());
        }
    }

    @Test
    void secretMissionIsLimitedToOnePerPlayer() {
        ObjectiveRegistry registry = DefaultObjectiveCatalog.createRegistry();
        ObjectiveEngine engine = new ObjectiveEngine();
        PlayerId player = players(1).getFirst();

        engine.assign(
                player,
                registry.require(new ObjectiveId("survive_return")),
                ObjectiveSlot.BASE
        );

        SecretMissionService secrets = new SecretMissionService(engine);
        assertEquals(
                SecretMissionGrantResult.GRANTED,
                secrets.grantRandom(player, List.copyOf(registry.all()), new Random(1))
        );
        assertEquals(
                SecretMissionGrantResult.ALREADY_HAS_SECRET,
                secrets.grantRandom(player, List.copyOf(registry.all()), new Random(2))
        );
    }

    @Test
    void objectiveProgressCompletesAndScores() {
        ObjectiveRegistry registry = DefaultObjectiveCatalog.createRegistry();
        ObjectiveEngine engine = new ObjectiveEngine();
        PlayerId player = players(1).getFirst();

        ObjectiveInstance instance = engine.assign(
                player,
                registry.require(new ObjectiveId("collect_data_cores")),
                ObjectiveSlot.BASE
        );

        engine.advance(instance.instanceId(), 1);
        assertEquals(ObjectiveStatus.ACTIVE, instance.status());

        engine.advance(instance.instanceId(), 1);
        assertEquals(ObjectiveStatus.COMPLETED, instance.status());
        assertEquals(100, engine.completedScore(player));
    }

    private static List<PlayerId> players(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> PlayerId.of(UUID.nameUUIDFromBytes(("player-" + i).getBytes())))
                .toList();
    }
}
