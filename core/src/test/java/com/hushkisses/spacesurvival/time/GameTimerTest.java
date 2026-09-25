package com.hushkisses.spacesurvival.time;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

class GameTimerTest {

    @Test
    void reportsElapsedAndRemainingTime() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-25T00:00:00Z"));
        GameTimer timer = new GameTimer(Duration.ofMinutes(45), clock);

        timer.start();
        clock.advance(Duration.ofMinutes(12));

        MatchTimeSnapshot snapshot = timer.snapshot();

        assertEquals(Duration.ofMinutes(12), snapshot.elapsed());
        assertEquals(Duration.ofMinutes(33), snapshot.remainingToTarget());
        assertFalse(snapshot.targetReached());
        assertTrue(snapshot.running());
    }

    @Test
    void targetCanBeExceededWithoutStoppingTimer() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-25T00:00:00Z"));
        GameTimer timer = new GameTimer(Duration.ofMinutes(45), clock);

        timer.start();
        clock.advance(Duration.ofMinutes(55));

        MatchTimeSnapshot snapshot = timer.snapshot();

        assertTrue(snapshot.targetReached());
        assertEquals(Duration.ZERO, snapshot.remainingToTarget());
        assertTrue(snapshot.running());
    }

    @Test
    void stopFreezesElapsedTime() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-25T00:00:00Z"));
        GameTimer timer = new GameTimer(Duration.ofMinutes(45), clock);

        timer.start();
        clock.advance(Duration.ofMinutes(10));
        timer.stop();
        clock.advance(Duration.ofMinutes(20));

        MatchTimeSnapshot snapshot = timer.snapshot();

        assertEquals(Duration.ofMinutes(10), snapshot.elapsed());
        assertFalse(snapshot.running());
    }

    @Test
    void cannotStartTwice() {
        GameTimer timer = new GameTimer(Duration.ofMinutes(45));
        timer.start();

        assertThrows(IllegalStateException.class, timer::start);
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
