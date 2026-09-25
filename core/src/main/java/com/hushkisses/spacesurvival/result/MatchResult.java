package com.hushkisses.spacesurvival.result;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record MatchResult(
        boolean commonMissionCompleted,
        List<PlayerMatchResult> players,
        Set<PlayerId> winners,
        Set<PlayerId> mvps
) {
    public MatchResult {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(winners, "winners");
        Objects.requireNonNull(mvps, "mvps");
        players = List.copyOf(players);
        winners = Set.copyOf(winners);
        mvps = Set.copyOf(mvps);
    }
}
