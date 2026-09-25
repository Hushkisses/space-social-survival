package com.hushkisses.spacesurvival.event;

import java.time.Clock;
import java.util.Objects;

public final class GameEventEngine {

    private final Clock clock;

    public GameEventEngine() {
        this(Clock.systemUTC());
    }

    public GameEventEngine(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void trigger(GameEventDefinition definition, GameEventContext context) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(context, "context");

        definition.effects().forEach(effect -> effect.apply(context));
        context.runtimeState().record(definition.id(), clock.instant());
    }
}
