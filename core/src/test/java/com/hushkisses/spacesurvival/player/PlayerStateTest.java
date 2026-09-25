package com.hushkisses.spacesurvival.player;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateTest {

    private static final PlayerId PLAYER_ID = PlayerId.of(
            UUID.fromString("00000000-0000-0000-0000-000000000001")
    );
    private static final Instant JOINED = Instant.parse("2026-09-25T00:00:00Z");

    @Test
    void newPlayerStartsAliveAndConnected() {
        PlayerState state = stateAt(JOINED);

        assertEquals(PLAYER_ID, state.id());
        assertTrue(state.isAlive());
        assertTrue(state.isConnected());
        assertEquals(JOINED, state.joinedAt());
        assertEquals(JOINED, state.connectionChangedAt());
        assertNull(state.diedAt());
    }

    @Test
    void disconnectAndReconnectOnlyChangeConnectivity() {
        MutableClock clock = new MutableClock(JOINED);
        PlayerState state = PlayerState.create(PLAYER_ID, clock);

        Instant disconnectedAt = JOINED.plusSeconds(5);
        clock.setInstant(disconnectedAt);
        state.disconnect();

        assertFalse(state.isConnected());
        assertTrue(state.isAlive());
        assertEquals(disconnectedAt, state.connectionChangedAt());

        Instant reconnectedAt = JOINED.plusSeconds(12);
        clock.setInstant(reconnectedAt);
        state.reconnect();

        assertTrue(state.isConnected());
        assertTrue(state.isAlive());
        assertEquals(reconnectedAt, state.connectionChangedAt());
    }

    @Test
    void connectivityChangesAreIdempotent() {
        MutableClock clock = new MutableClock(JOINED);
        PlayerState state = PlayerState.create(PLAYER_ID, clock);

        Instant firstDisconnect = JOINED.plusSeconds(5);
        clock.setInstant(firstDisconnect);
        state.disconnect();

        clock.setInstant(JOINED.plusSeconds(20));
        state.disconnect();

        assertEquals(firstDisconnect, state.connectionChangedAt());

        Instant firstReconnect = JOINED.plusSeconds(25);
        clock.setInstant(firstReconnect);
        state.reconnect();

        clock.setInstant(JOINED.plusSeconds(30));
        state.reconnect();

        assertEquals(firstReconnect, state.connectionChangedAt());
    }

    @Test
    void deathIsTerminalAndTimestampRecordedOnce() {
        MutableClock clock = new MutableClock(JOINED);
        PlayerState state = PlayerState.create(PLAYER_ID, clock);

        Instant diedAt = JOINED.plusSeconds(10);
        clock.setInstant(diedAt);
        state.markDead();

        assertFalse(state.isAlive());
        assertEquals(diedAt, state.diedAt());

        clock.setInstant(JOINED.plusSeconds(30));
        state.markDead();

        assertFalse(state.isAlive());
        assertEquals(diedAt, state.diedAt());
    }

    @Test
    void disconnectDoesNotReviveDeadPlayer() {
        PlayerState state = stateAt(JOINED);

        state.markDead();
        state.disconnect();
        state.reconnect();

        assertFalse(state.isAlive());
        assertTrue(state.isConnected());
    }

    private static PlayerState stateAt(Instant instant) {
        return PlayerState.create(PLAYER_ID, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            if (!ZoneOffset.UTC.equals(zone)) {
                throw new UnsupportedOperationException("Only UTC is supported in this test clock");
            }
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
