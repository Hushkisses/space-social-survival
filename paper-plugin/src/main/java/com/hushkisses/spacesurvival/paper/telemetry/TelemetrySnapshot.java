package com.hushkisses.spacesurvival.paper.telemetry;

import java.time.Instant;
import java.util.Map;

public record TelemetrySnapshot(
        boolean active,
        long seed,
        int playerCount,
        String scenario,
        Instant startedAt,
        Map<String, Long> counters,
        int eventCount,
        String lastSavedFile
) {
    public TelemetrySnapshot {
        counters = Map.copyOf(counters);
    }
}
