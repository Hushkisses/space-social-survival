package com.hushkisses.spacesurvival.social.sanction;

import java.util.Map;

public record SanctionVoteResult(
        SanctionChoice winningChoice,
        boolean tied,
        Map<SanctionChoice, Integer> counts
) {
    public SanctionVoteResult {
        counts = Map.copyOf(counts);
    }
}
