package com.hushkisses.spacesurvival.ship;

import com.hushkisses.spacesurvival.time.CrisisFactors;

import java.util.Objects;

public final class ShipCrisisPressure {

    public CrisisFactors evaluate(ShipStateSnapshot ship) {
        Objects.requireNonNull(ship, "ship");

        int pressure = metricPressure(ship.power())
                + metricPressure(ship.oxygen())
                + metricPressure(ship.hull())
                + metricPressure(ship.reactor());

        return new CrisisFactors(pressure);
    }

    private static int metricPressure(int value) {
        if (value >= 85) return -1;
        if (value >= 70) return 0;
        if (value >= 50) return 2;
        if (value >= 30) return 5;
        if (value >= 15) return 9;
        return 15;
    }
}
