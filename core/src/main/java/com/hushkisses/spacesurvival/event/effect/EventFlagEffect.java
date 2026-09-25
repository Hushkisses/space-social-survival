package com.hushkisses.spacesurvival.event.effect;

import com.hushkisses.spacesurvival.event.GameEventContext;

import java.util.Objects;

public record EventFlagEffect(String flag, boolean active) implements EventEffect {
    public EventFlagEffect {
        Objects.requireNonNull(flag, "flag");
        if (flag.isBlank()) throw new IllegalArgumentException("flag");
    }

    @Override
    public void apply(GameEventContext context) {
        context.runtimeState().setFlag(flag, active);
    }
}
