package com.hushkisses.spacesurvival.event.effect;

import com.hushkisses.spacesurvival.event.GameEventContext;
import com.hushkisses.spacesurvival.ship.ShipMetric;

import java.util.Objects;

public record ShipMetricDeltaEffect(ShipMetric metric, int delta) implements EventEffect {
    public ShipMetricDeltaEffect {
        Objects.requireNonNull(metric, "metric");
        if (delta == 0) throw new IllegalArgumentException("delta");
    }

    @Override
    public void apply(GameEventContext context) {
        int current = context.shipState().value(metric);
        int next = Math.max(0, Math.min(100, current + delta));
        context.shipState().set(metric, next);
    }
}
