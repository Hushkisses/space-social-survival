package com.hushkisses.spacesurvival.resource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ResourceStore {

    private final EnumMap<ResourceType, Integer> quantities = new EnumMap<>(ResourceType.class);

    public int quantity(ResourceType type) {
        return quantities.getOrDefault(Objects.requireNonNull(type, "type"), 0);
    }

    public void add(ResourceType type, int amount) {
        Objects.requireNonNull(type, "type");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }
        quantities.merge(type, amount, Math::addExact);
    }

    public boolean remove(ResourceType type, int amount) {
        Objects.requireNonNull(type, "type");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be positive");
        }

        int current = quantity(type);
        if (current < amount) {
            return false;
        }

        int next = current - amount;
        if (next == 0) {
            quantities.remove(type);
        } else {
            quantities.put(type, next);
        }
        return true;
    }

    public boolean has(ResourceType type, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        return quantity(type) >= amount;
    }

    public Map<ResourceType, Integer> snapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(quantities));
    }

    public boolean transferTo(ResourceStore target, ResourceType type, int amount) {
        Objects.requireNonNull(target, "target");
        if (!remove(type, amount)) {
            return false;
        }
        target.add(type, amount);
        return true;
    }
}
