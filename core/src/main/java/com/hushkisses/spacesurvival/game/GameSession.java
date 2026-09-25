package com.hushkisses.spacesurvival.game;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GameSession {

    private static final Map<GamePhase, Set<GamePhase>> ALLOWED_TRANSITIONS = Map.of(
            GamePhase.WAITING, EnumSet.of(GamePhase.PREPARING),
            GamePhase.PREPARING, EnumSet.of(GamePhase.BRIEFING, GamePhase.FINISHED),
            GamePhase.BRIEFING, EnumSet.of(GamePhase.ACTIVE, GamePhase.FINISHED),
            GamePhase.ACTIVE, EnumSet.of(GamePhase.RETURN_PHASE, GamePhase.FINISHED),
            GamePhase.RETURN_PHASE, EnumSet.of(GamePhase.FINISHED),
            GamePhase.FINISHED, EnumSet.noneOf(GamePhase.class)
    );

    private final GameSessionId id;
    private final Clock clock;
    private final Instant createdAt;
    private GamePhase phase;
    private Instant phaseChangedAt;

    private GameSession(GameSessionId id, Clock clock) {
        this.id = Objects.requireNonNull(id, "id");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.createdAt = clock.instant();
        this.phase = GamePhase.WAITING;
        this.phaseChangedAt = createdAt;
    }

    public static GameSession create() {
        return new GameSession(GameSessionId.random(), Clock.systemUTC());
    }

    public static GameSession create(GameSessionId id, Clock clock) {
        return new GameSession(id, clock);
    }

    public GameSessionId id() {
        return id;
    }

    public GamePhase phase() {
        return phase;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant phaseChangedAt() {
        return phaseChangedAt;
    }

    public boolean isFinished() {
        return phase == GamePhase.FINISHED;
    }

    public boolean canTransitionTo(GamePhase target) {
        Objects.requireNonNull(target, "target");
        return ALLOWED_TRANSITIONS.get(phase).contains(target);
    }

    public void transitionTo(GamePhase target) {
        Objects.requireNonNull(target, "target");

        if (!canTransitionTo(target)) {
            throw new InvalidGamePhaseTransitionException(phase, target);
        }

        phase = target;
        phaseChangedAt = clock.instant();
    }
}
