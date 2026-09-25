package com.hushkisses.spacesurvival.result;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.*;

public final class ResultEvaluator {

    private final ResultScoringConfig config;

    public ResultEvaluator(ResultScoringConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public MatchResult evaluate(
            Collection<PlayerResultInput> inputs,
            boolean commonMissionCompleted
    ) {
        Objects.requireNonNull(inputs, "inputs");

        ArrayList<PlayerMatchResult> results = new ArrayList<>();
        LinkedHashSet<PlayerId> winners = new LinkedHashSet<>();

        for (PlayerResultInput input : inputs) {
            int contribution = Math.min(
                    config.maxCommonContribution(),
                    input.commonContribution()
            );

            ScoreBreakdown score = new ScoreBreakdown(
                    input.alive() ? config.survivalPoints() : 0,
                    input.baseObjectiveCompleted() ? config.baseObjectivePoints() : 0,
                    input.secretObjectiveCompleted() ? config.secretObjectivePoints() : 0,
                    contribution,
                    input.scenarioBonus()
            );

            boolean winner = commonMissionCompleted && input.baseObjectiveCompleted();
            PlayerMatchResult result = new PlayerMatchResult(input.playerId(), winner, score);
            results.add(result);

            if (winner) {
                winners.add(input.playerId());
            }
        }

        LinkedHashSet<PlayerId> mvps = new LinkedHashSet<>();
        int topScore = results.stream()
                .filter(PlayerMatchResult::winner)
                .mapToInt(result -> result.score().total())
                .max()
                .orElse(Integer.MIN_VALUE);

        if (topScore != Integer.MIN_VALUE) {
            results.stream()
                    .filter(PlayerMatchResult::winner)
                    .filter(result -> result.score().total() == topScore)
                    .map(PlayerMatchResult::playerId)
                    .forEach(mvps::add);
        }

        return new MatchResult(commonMissionCompleted, results, winners, mvps);
    }
}
