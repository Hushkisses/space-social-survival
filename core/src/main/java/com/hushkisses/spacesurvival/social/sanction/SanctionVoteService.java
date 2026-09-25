package com.hushkisses.spacesurvival.social.sanction;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.social.meeting.MeetingSession;

import java.util.*;

public final class SanctionVoteService {

    private final MeetingSession meeting;
    private final Map<PlayerId, SanctionChoice> votes = new LinkedHashMap<>();
    private boolean resolved;

    public SanctionVoteService(MeetingSession meeting) {
        this.meeting = Objects.requireNonNull(meeting, "meeting");
    }

    public void vote(PlayerId voter, SanctionChoice choice) {
        Objects.requireNonNull(voter, "voter");
        Objects.requireNonNull(choice, "choice");

        if (resolved) {
            throw new IllegalStateException("Vote is already resolved");
        }
        if (!meeting.participants().contains(voter)) {
            throw new IllegalArgumentException("Voter is not a meeting participant");
        }
        if (choice.target() != null && !meeting.participants().contains(choice.target())) {
            throw new IllegalArgumentException("Target is not a meeting participant");
        }

        votes.put(voter, choice);
    }

    public Map<PlayerId, SanctionChoice> votes() {
        return Collections.unmodifiableMap(votes);
    }

    public SanctionVoteResult resolve() {
        if (resolved) {
            throw new IllegalStateException("Vote is already resolved");
        }
        resolved = true;

        Map<SanctionChoice, Integer> counts = new LinkedHashMap<>();
        votes.values().forEach(choice -> counts.merge(choice, 1, Integer::sum));

        if (counts.isEmpty()) {
            return new SanctionVoteResult(SanctionChoice.noAction(), false, counts);
        }

        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<SanctionChoice> leaders = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        if (leaders.size() != 1) {
            return new SanctionVoteResult(SanctionChoice.noAction(), true, counts);
        }

        return new SanctionVoteResult(leaders.getFirst(), false, counts);
    }
}
