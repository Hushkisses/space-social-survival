package com.hushkisses.spacesurvival.ship;

import java.util.Objects;

public final class ShipState {

    private int power;
    private int oxygen;
    private int hull;
    private int reactor;

    public ShipState(
            int power,
            int oxygen,
            int hull,
            int reactor
    ) {
        this.power = validatePercent(power, "power");
        this.oxygen = validatePercent(oxygen, "oxygen");
        this.hull = validatePercent(hull, "hull");
        this.reactor = validatePercent(reactor, "reactor");
    }

    public static ShipState healthy() {
        return new ShipState(100, 100, 100, 100);
    }

    public int power() {
        return power;
    }

    public int oxygen() {
        return oxygen;
    }

    public int hull() {
        return hull;
    }

    public int reactor() {
        return reactor;
    }

    public int value(ShipMetric metric) {
        Objects.requireNonNull(metric, "metric");
        return switch (metric) {
            case POWER -> power;
            case OXYGEN -> oxygen;
            case HULL -> hull;
            case REACTOR -> reactor;
        };
    }

    public void set(ShipMetric metric, int value) {
        Objects.requireNonNull(metric, "metric");
        int validated = validatePercent(value, metric.name().toLowerCase());

        switch (metric) {
            case POWER -> power = validated;
            case OXYGEN -> oxygen = validated;
            case HULL -> hull = validated;
            case REACTOR -> reactor = validated;
        }
    }

    public void reset() {
        power = 100;
        oxygen = 100;
        hull = 100;
        reactor = 100;
    }

    public ShipStateSnapshot snapshot() {
        return new ShipStateSnapshot(power, oxygen, hull, reactor);
    }

    private static int validatePercent(int value, String name) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
        return value;
    }
}
