package com.hushkisses.spacesurvival.paper.ending;

import com.hushkisses.spacesurvival.ending.CommonContributionLedger;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.objective.ObjectiveStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.result.*;

import java.util.*;

public final class MatchResultRuntimeService {

    private final SpaceSurvivalPlugin plugin;
    private final CommonContributionLedger contributions;
    private final ResultEvaluator evaluator;
    private final Map<PlayerId, Integer> scenarioBonuses = new LinkedHashMap<>();

    private MatchResult lastResult;

    public MatchResultRuntimeService(
            SpaceSurvivalPlugin plugin,
            CommonContributionLedger contributions,
            ResultEvaluator evaluator
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.contributions = Objects.requireNonNull(contributions, "contributions");
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public CommonContributionLedger contributions() {
        return contributions;
    }

    public void setScenarioBonus(PlayerId playerId, int bonus) {
        Objects.requireNonNull(playerId, "playerId");
        scenarioBonuses.put(playerId, bonus);
    }

    public int scenarioBonus(PlayerId playerId) {
        return scenarioBonuses.getOrDefault(playerId, 0);
    }

    public MatchResult evaluate(boolean commonMissionCompleted) {
        List<PlayerResultInput> inputs = plugin.lobbyService().snapshot().players().stream()
                .map(this::input)
                .toList();

        lastResult = evaluator.evaluate(inputs, commonMissionCompleted);
        return lastResult;
    }

    public Optional<MatchResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    private PlayerResultInput input(PlayerId playerId) {
        boolean alive = plugin.lobbyService().playerState(playerId)
                .map(state -> state.isAlive())
                .orElse(false);

        boolean baseCompleted = plugin.objectiveEngine()
                .objective(playerId, ObjectiveSlot.BASE)
                .map(objective -> objective.status() == ObjectiveStatus.COMPLETED)
                .orElse(false);

        boolean secretCompleted = plugin.objectiveEngine()
                .objective(playerId, ObjectiveSlot.SECRET)
                .map(objective -> objective.status() == ObjectiveStatus.COMPLETED)
                .orElse(false);

        return new PlayerResultInput(
                playerId,
                alive,
                baseCompleted,
                secretCompleted,
                contributions.get(playerId),
                scenarioBonus(playerId)
        );
    }
}
