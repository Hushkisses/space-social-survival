package com.hushkisses.spacesurvival.ship;

public record ShipStateSnapshot(
        int power,
        int oxygen,
        int hull,
        int reactor
) {

    public ShipStateSnapshot {
        validate(power, "power");
        validate(oxygen, "oxygen");
        validate(hull, "hull");
        validate(reactor, "reactor");
    }

    public int value(ShipMetric metric) {
        return switch (metric) {
            case POWER -> power;
            case OXYGEN -> oxygen;
            case HULL -> hull;
            case REACTOR -> reactor;
        };
    }

    private static void validate(int value, String name) {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException(name + " must be between 0 and 100");
        }
    }
}
