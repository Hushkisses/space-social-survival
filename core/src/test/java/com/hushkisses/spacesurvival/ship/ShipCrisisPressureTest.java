package com.hushkisses.spacesurvival.ship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipCrisisPressureTest {

    private final ShipCrisisPressure pressure = new ShipCrisisPressure();

    @Test
    void healthyShipOffsetsSomeTimePressure() {
        assertEquals(-4, pressure.evaluate(ShipState.healthy().snapshot()).externalPressure());
    }

    @Test
    void degradedShipAddsPressure() {
        ShipState ship = new ShipState(55, 65, 45, 80);
        assertTrue(pressure.evaluate(ship.snapshot()).externalPressure() > 0);
    }

    @Test
    void criticalShipAddsLargePressure() {
        ShipState ship = new ShipState(10, 10, 20, 10);
        assertTrue(pressure.evaluate(ship.snapshot()).externalPressure() >= 40);
    }
}
