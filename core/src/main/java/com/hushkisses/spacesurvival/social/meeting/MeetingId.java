package com.hushkisses.spacesurvival.social.meeting;

import java.util.Objects;
import java.util.UUID;

public record MeetingId(UUID value) {
    public MeetingId {
        Objects.requireNonNull(value, "value");
    }

    public static MeetingId random() {
        return new MeetingId(UUID.randomUUID());
    }
}
