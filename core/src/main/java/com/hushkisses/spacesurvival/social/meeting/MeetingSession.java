package com.hushkisses.spacesurvival.social.meeting;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.time.Instant;
import java.util.*;

public final class MeetingSession {

    private final MeetingId id;
    private final MeetingType type;
    private final EmergencyMeetingReason emergencyReason;
    private final Set<PlayerId> participants;
    private final Instant startedAt;
    private MeetingStatus status = MeetingStatus.ACTIVE;
    private Instant endedAt;

    public MeetingSession(
            MeetingId id,
            MeetingType type,
            EmergencyMeetingReason emergencyReason,
            Collection<PlayerId> participants,
            Instant startedAt
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.emergencyReason = emergencyReason;
        Objects.requireNonNull(participants, "participants");
        if (participants.isEmpty()) throw new IllegalArgumentException("participants");
        this.participants = Collections.unmodifiableSet(new LinkedHashSet<>(participants));
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");

        if (type == MeetingType.EMERGENCY && emergencyReason == null) {
            throw new IllegalArgumentException("emergencyReason required");
        }
        if (type == MeetingType.REGULAR && emergencyReason != null) {
            throw new IllegalArgumentException("regular meeting cannot have emergencyReason");
        }
    }

    public MeetingId id() { return id; }
    public MeetingType type() { return type; }
    public Optional<EmergencyMeetingReason> emergencyReason() {
        return Optional.ofNullable(emergencyReason);
    }
    public Set<PlayerId> participants() { return participants; }
    public Instant startedAt() { return startedAt; }
    public MeetingStatus status() { return status; }
    public Optional<Instant> endedAt() { return Optional.ofNullable(endedAt); }

    public void resolve(Instant at) {
        ensureActive();
        status = MeetingStatus.RESOLVED;
        endedAt = Objects.requireNonNull(at, "at");
    }

    public void cancel(Instant at) {
        ensureActive();
        status = MeetingStatus.CANCELLED;
        endedAt = Objects.requireNonNull(at, "at");
    }

    private void ensureActive() {
        if (status != MeetingStatus.ACTIVE) {
            throw new IllegalStateException("Meeting is not active");
        }
    }
}
