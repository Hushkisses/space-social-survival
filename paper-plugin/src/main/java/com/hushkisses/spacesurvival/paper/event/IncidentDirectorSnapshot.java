package com.hushkisses.spacesurvival.paper.event;

public record IncidentDirectorSnapshot(
        boolean running,
        long nextSmallAtSeconds,
        long nextMajorAtSeconds,
        int majorEventsTriggered,
        String lastEventId
) {
}
