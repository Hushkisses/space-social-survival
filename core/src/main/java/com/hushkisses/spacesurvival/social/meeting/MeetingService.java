package com.hushkisses.spacesurvival.social.meeting;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.ship.ShipState;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class MeetingService {

    private final FacilityRegistry facilities;
    private final ShipState shipState;
    private final Duration regularCooldown;
    private final Clock clock;

    private MeetingSession activeMeeting;
    private Instant lastMeetingEndedAt;

    public MeetingService(
            FacilityRegistry facilities,
            ShipState shipState,
            Duration regularCooldown
    ) {
        this(facilities, shipState, regularCooldown, Clock.systemUTC());
    }

    public MeetingService(
            FacilityRegistry facilities,
            ShipState shipState,
            Duration regularCooldown,
            Clock clock
    ) {
        this.facilities = Objects.requireNonNull(facilities, "facilities");
        this.shipState = Objects.requireNonNull(shipState, "shipState");
        this.regularCooldown = Objects.requireNonNull(regularCooldown, "regularCooldown");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (regularCooldown.isNegative()) {
            throw new IllegalArgumentException("regularCooldown must not be negative");
        }
    }

    public Optional<MeetingSession> activeMeeting() {
        return Optional.ofNullable(activeMeeting);
    }

    public MeetingStartResult startRegular(
            Collection<PlayerId> participants,
            boolean communicationsAvailable
    ) {
        MeetingStartDenialReason common = commonDenial(participants);
        if (common != null) return MeetingStartResult.denied(common);

        FacilityStatus bridgeStatus = facilities.require(DefaultFacilityCatalog.BRIDGE).status();
        if (bridgeStatus == FacilityStatus.OFFLINE || bridgeStatus == FacilityStatus.QUARANTINED) {
            return MeetingStartResult.denied(MeetingStartDenialReason.BRIDGE_UNAVAILABLE);
        }

        if (shipState.power() <= 0 && !communicationsAvailable) {
            return MeetingStartResult.denied(MeetingStartDenialReason.NO_POWER_OR_COMMUNICATION);
        }

        Instant now = clock.instant();
        if (lastMeetingEndedAt != null
                && now.isBefore(lastMeetingEndedAt.plus(regularCooldown))) {
            return MeetingStartResult.denied(MeetingStartDenialReason.COOLDOWN_ACTIVE);
        }

        activeMeeting = new MeetingSession(
                MeetingId.random(),
                MeetingType.REGULAR,
                null,
                participants,
                now
        );
        return MeetingStartResult.started(activeMeeting);
    }

    public MeetingStartResult startEmergency(
            Collection<PlayerId> participants,
            EmergencyMeetingReason reason
    ) {
        Objects.requireNonNull(reason, "reason");
        MeetingStartDenialReason common = commonDenial(participants);
        if (common != null) return MeetingStartResult.denied(common);

        activeMeeting = new MeetingSession(
                MeetingId.random(),
                MeetingType.EMERGENCY,
                reason,
                participants,
                clock.instant()
        );
        return MeetingStartResult.started(activeMeeting);
    }

    public void resolveActive() {
        MeetingSession meeting = requireActive();
        meeting.resolve(clock.instant());
        lastMeetingEndedAt = meeting.endedAt().orElseThrow();
        activeMeeting = null;
    }

    public void cancelActive() {
        MeetingSession meeting = requireActive();
        meeting.cancel(clock.instant());
        lastMeetingEndedAt = meeting.endedAt().orElseThrow();
        activeMeeting = null;
    }

    public Duration remainingCooldown() {
        if (lastMeetingEndedAt == null) return Duration.ZERO;
        Instant availableAt = lastMeetingEndedAt.plus(regularCooldown);
        Duration remaining = Duration.between(clock.instant(), availableAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    private MeetingStartDenialReason commonDenial(Collection<PlayerId> participants) {
        Objects.requireNonNull(participants, "participants");
        if (activeMeeting != null) {
            return MeetingStartDenialReason.MEETING_ALREADY_ACTIVE;
        }
        if (participants.isEmpty()) {
            return MeetingStartDenialReason.NO_PARTICIPANTS;
        }
        return null;
    }

    private MeetingSession requireActive() {
        if (activeMeeting == null) {
            throw new IllegalStateException("No active meeting");
        }
        return activeMeeting;
    }
}
