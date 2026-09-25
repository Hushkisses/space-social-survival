package com.hushkisses.spacesurvival.event;

import java.time.Instant;
import java.util.*;

public final class GameEventRuntimeState {

    private final Set<String> activeFlags = new LinkedHashSet<>();
    private final List<GameEventHistoryEntry> history = new ArrayList<>();

    public void setFlag(String flag, boolean active) {
        Objects.requireNonNull(flag, "flag");
        if (flag.isBlank()) throw new IllegalArgumentException("flag");
        if (active) activeFlags.add(flag);
        else activeFlags.remove(flag);
    }

    public boolean hasFlag(String flag) {
        return activeFlags.contains(flag);
    }

    public Set<String> activeFlags() {
        return Collections.unmodifiableSet(activeFlags);
    }

    public void record(GameEventId eventId, Instant at) {
        history.add(new GameEventHistoryEntry(eventId, at));
    }

    public List<GameEventHistoryEntry> history() {
        return List.copyOf(history);
    }

    public record GameEventHistoryEntry(GameEventId eventId, Instant triggeredAt) {
        public GameEventHistoryEntry {
            Objects.requireNonNull(eventId, "eventId");
            Objects.requireNonNull(triggeredAt, "triggeredAt");
        }
    }
}
