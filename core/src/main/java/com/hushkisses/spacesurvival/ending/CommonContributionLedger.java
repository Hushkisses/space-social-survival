package com.hushkisses.spacesurvival.ending;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CommonContributionLedger {

    private final int maxContribution;
    private final Map<PlayerId, Integer> contributions = new LinkedHashMap<>();

    public CommonContributionLedger(int maxContribution) {
        if (maxContribution < 1) {
            throw new IllegalArgumentException("maxContribution must be positive");
        }
        this.maxContribution = maxContribution;
    }

    public void set(PlayerId playerId, int contribution) {
        Objects.requireNonNull(playerId, "playerId");
        if (contribution < 0 || contribution > maxContribution) {
            throw new IllegalArgumentException("contribution out of range");
        }
        contributions.put(playerId, contribution);
    }

    public void add(PlayerId playerId, int amount) {
        Objects.requireNonNull(playerId, "playerId");
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        set(playerId, Math.min(maxContribution, get(playerId) + amount));
    }

    public int get(PlayerId playerId) {
        return contributions.getOrDefault(Objects.requireNonNull(playerId, "playerId"), 0);
    }
}
