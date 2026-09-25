package com.hushkisses.spacesurvival.social;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityRegistry;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.ship.ShipState;
import com.hushkisses.spacesurvival.social.meeting.*;
import com.hushkisses.spacesurvival.social.pvp.*;
import com.hushkisses.spacesurvival.social.sanction.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SocialSystemsBatchTest {

    @Test
    void regularMeetingChecksInfrastructureAndCooldown() {
        FacilityRegistry facilities = DefaultFacilityCatalog.createRegistry();
        ShipState ship = ShipState.healthy();
        MutableClock clock = new MutableClock(Instant.parse("2026-09-25T00:00:00Z"));
        MeetingService service = new MeetingService(
                facilities,
                ship,
                Duration.ofMinutes(3),
                clock
        );
        List<PlayerId> players = players();

        assertTrue(service.startRegular(players, true).started());
        service.resolveActive();

        assertEquals(
                MeetingStartDenialReason.COOLDOWN_ACTIVE,
                service.startRegular(players, true).denialReason()
        );

        clock.advance(Duration.ofMinutes(3));
        facilities.require(DefaultFacilityCatalog.BRIDGE)
                .setStatus(FacilityStatus.OFFLINE);
        assertEquals(
                MeetingStartDenialReason.BRIDGE_UNAVAILABLE,
                service.startRegular(players, true).denialReason()
        );

        facilities.require(DefaultFacilityCatalog.BRIDGE)
                .setStatus(FacilityStatus.NORMAL);
        ship.set(com.hushkisses.spacesurvival.ship.ShipMetric.POWER, 0);
        assertEquals(
                MeetingStartDenialReason.NO_POWER_OR_COMMUNICATION,
                service.startRegular(players, false).denialReason()
        );
    }

    @Test
    void emergencyMeetingBypassesInfrastructureAndCooldown() {
        FacilityRegistry facilities = DefaultFacilityCatalog.createRegistry();
        ShipState ship = ShipState.healthy();
        facilities.require(DefaultFacilityCatalog.BRIDGE)
                .setStatus(FacilityStatus.OFFLINE);
        MeetingService service = new MeetingService(
                facilities,
                ship,
                Duration.ofHours(1)
        );

        MeetingStartResult result = service.startEmergency(
                players(),
                EmergencyMeetingReason.REACTOR_CRITICAL
        );

        assertTrue(result.started());
        assertEquals(MeetingType.EMERGENCY, result.meeting().type());
    }

    @Test
    void sanctionVoteTieFallsBackToNoAction() {
        List<PlayerId> players = players();
        MeetingSession meeting = new MeetingSession(
                MeetingId.random(),
                MeetingType.REGULAR,
                null,
                players,
                Instant.now()
        );
        SanctionVoteService vote = new SanctionVoteService(meeting);

        vote.vote(players.get(0), new SanctionChoice(SanctionType.DISARM, players.get(1)));
        vote.vote(players.get(1), SanctionChoice.noAction());

        SanctionVoteResult result = vote.resolve();

        assertTrue(result.tied());
        assertEquals(SanctionType.NO_ACTION, result.winningChoice().sanction());
    }

    @Test
    void sanctionExecutionRequiresRealPrerequisites() {
        PlayerId target = players().getFirst();
        SanctionStateRegistry registry = new SanctionStateRegistry();
        SanctionExecutor executor = new SanctionExecutor(registry);

        SanctionExecutionResult failed = executor.execute(
                new SanctionChoice(SanctionType.DISARM, target),
                new SanctionExecutionContext(true, false, true, true)
        );
        assertFalse(failed.executed());
        assertFalse(registry.state(target).disarmed());

        assertTrue(executor.execute(
                new SanctionChoice(SanctionType.DISARM, target),
                new SanctionExecutionContext(true, true, true, true)
        ).executed());
        assertTrue(registry.state(target).disarmed());
    }

    @Test
    void pvpIsConditional() {
        ConditionalPvpPolicy policy = new ConditionalPvpPolicy();

        assertFalse(policy.isAllowed(
                new PvpPermissionContext(false, false, false, false, false, false)
        ));
        assertTrue(policy.isAllowed(
                new PvpPermissionContext(true, true, false, false, false, false)
        ));
        assertTrue(policy.isAllowed(
                new PvpPermissionContext(false, false, true, false, false, false)
        ));
        assertTrue(policy.isAllowed(
                new PvpPermissionContext(false, false, false, false, true, false)
        ));
    }

    private static List<PlayerId> players() {
        return List.of(
                PlayerId.of(UUID.nameUUIDFromBytes("p1".getBytes())),
                PlayerId.of(UUID.nameUUIDFromBytes("p2".getBytes())),
                PlayerId.of(UUID.nameUUIDFromBytes("p3".getBytes()))
        );
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
