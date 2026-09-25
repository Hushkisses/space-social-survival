package com.hushkisses.spacesurvival.event.effect;

import com.hushkisses.spacesurvival.event.GameEventContext;
import com.hushkisses.spacesurvival.resource.ResourceStore;
import com.hushkisses.spacesurvival.resource.ResourceType;

import java.util.Objects;

public record SharedResourceDeltaEffect(
        ResourceType resourceType,
        int delta
) implements EventEffect {
    public SharedResourceDeltaEffect {
        Objects.requireNonNull(resourceType, "resourceType");
        if (delta == 0) throw new IllegalArgumentException("delta");
    }

    @Override
    public void apply(GameEventContext context) {
        ResourceStore store = context.resources().shared();
        if (delta > 0) {
            store.add(resourceType, delta);
        } else {
            int remove = Math.min(store.quantity(resourceType), Math.abs(delta));
            if (remove > 0) {
                store.remove(resourceType, remove);
            }
        }
    }
}
