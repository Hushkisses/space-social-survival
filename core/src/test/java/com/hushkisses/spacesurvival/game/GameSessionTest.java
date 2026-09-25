package com.hushkisses.spacesurvival.game;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    private static final Instant CREATED = Instant.parse("2026-09-25T00:00:00Z");

    @Test
    void newSessionStartsWaiting() {
        GameSession session = sessionAt(CREATED);

        assertEquals(GamePhase.WAITING, session.phase());
        assertEquals(CREATED, session.createdAt());
        assertEquals(CREATED, session.phaseChangedAt());
        assertFalse(session.isFinished());
    }

    @Test
    void followsNormalLifecycle() {
        GameSession session = sessionAt(CREATED);

        session.transitionTo(GamePhase.PREPARING);
        session.transitionTo(GamePhase.BRIEFING);
        session.transitionTo(GamePhase.ACTIVE);
        session.transitionTo(GamePhase.RETURN_PHASE);
        session.transitionTo(GamePhase.FINISHED);

        assertEquals(GamePhase.FINISHED, session.phase());
        assertTrue(session.isFinished());
    }

    @Test
    void startedPhasesMayFinishEarly() {
        assertCanFinishFrom(GamePhase.PREPARING);
        assertCanFinishFrom(GamePhase.BRIEFING);
        assertCanFinishFrom(GamePhase.ACTIVE);
        assertCanFinishFrom(GamePhase.RETURN_PHASE);
    }

    @Test
    void cannotSkipRequiredPhase() {
        GameSession session = sessionAt(CREATED);

        InvalidGamePhaseTransitionException exception = assertThrows(
                InvalidGamePhaseTransitionException.class,
                () -> session.transitionTo(GamePhase.ACTIVE)
        );

        assertEquals("Illegal game phase transition: WAITING -> ACTIVE", exception.getMessage());
        assertEquals(GamePhase.WAITING, session.phase());
    }

    @Test
    void waitingSessionCannotFinishDirectly() {
        GameSession session = sessionAt(CREATED);

        assertFalse(session.canTransitionTo(GamePhase.FINISHED));
        assertThrows(
                InvalidGamePhaseTransitionException.class,
                () -> session.transitionTo(GamePhase.FINISHED)
        );
    }

    @Test
    void finishedSessionIsTerminal() {
        GameSession session = sessionAt(CREATED);
        session.transitionTo(GamePhase.PREPARING);
        session.transitionTo(GamePhase.FINISHED);

        for (GamePhase target : GamePhase.values()) {
            assertFalse(session.canTransitionTo(target));
            assertThrows(
                    InvalidGamePhaseTransitionException.class,
                    () -> session.transitionTo(target)
            );
        }
    }

    @Test
    void phaseChangeTimestampUsesClock() {
        MutableClock clock = new MutableClock(CREATED);
        GameSession session = GameSession.create(
                new GameSessionId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                clock
        );

        Instant changed = CREATED.plusSeconds(15);
        clock.setInstant(changed);
        session.transitionTo(GamePhase.PREPARING);

        assertEquals(CREATED, session.createdAt());
        assertEquals(changed, session.phaseChangedAt());
    }

    @Test
    void sessionIdRemainsStable() {
        GameSessionId id = new GameSessionId(
                UUID.fromString("00000000-0000-0000-0000-000000000001")
        );
        GameSession session = GameSession.create(id, Clock.fixed(CREATED, ZoneOffset.UTC));

        session.transitionTo(GamePhase.PREPARING);
        session.transitionTo(GamePhase.BRIEFING);

        assertEquals(id, session.id());
    }

    private static void assertCanFinishFrom(GamePhase phase) {
        GameSession session = sessionAt(CREATED);

        if (phase.ordinal() >= GamePhase.PREPARING.ordinal()) {
            session.transitionTo(GamePhase.PREPARING);
        }
        if (phase.ordinal() >= GamePhase.BRIEFING.ordinal()) {
            session.transitionTo(GamePhase.BRIEFING);
        }
        if (phase.ordinal() >= GamePhase.ACTIVE.ordinal()) {
            session.transitionTo(GamePhase.ACTIVE);
        }
        if (phase.ordinal() >= GamePhase.RETURN_PHASE.ordinal()) {
            session.transitionTo(GamePhase.RETURN_PHASE);
        }

        session.transitionTo(GamePhase.FINISHED);

        assertTrue(session.isFinished());
    }

    private static GameSession sessionAt(Instant instant) {
        return GameSession.create(
                new GameSessionId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
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
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
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
