package com.hushkisses.spacesurvival.ship;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipStateTest {

    @Test
    void healthyFactoryCreatesFullMetrics() {
        ShipState ship = ShipState.healthy();
        assertEquals(100, ship.power());
        assertEquals(100, ship.oxygen());
        assertEquals(100, ship.hull());
        assertEquals(100, ship.reactor());
    }

    @Test
    void setChangesOnlySelectedMetric() {
        ShipState ship = ShipState.healthy();
        ship.set(ShipMetric.POWER, 42);
        assertEquals(42, ship.power());
        assertEquals(100, ship.oxygen());
    }

    @Test
    void invalidPercentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ShipState(101, 100, 100, 100));
        ShipState ship = ShipState.healthy();
        assertThrows(IllegalArgumentException.class, () -> ship.set(ShipMetric.OXYGEN, -1));
    }
}
