package com.hushkisses.spacesurvival.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BalanceConfigTest {

    @Test
    void acceptsPositiveValues() {
        BalanceConfig config = new BalanceConfig(45, 240);

        assertEquals(45, config.targetMatchMinutes());
        assertEquals(240, config.returnHoldSeconds());
    }

    @Test
    void rejectsInvalidTargetMatchMinutes() {
        assertThrows(InvalidConfigurationException.class, () -> new BalanceConfig(0, 240));
    }

    @Test
    void rejectsInvalidReturnHoldSeconds() {
        assertThrows(InvalidConfigurationException.class, () -> new BalanceConfig(45, 0));
    }
}
