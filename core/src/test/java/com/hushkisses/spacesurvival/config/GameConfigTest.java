package com.hushkisses.spacesurvival.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void acceptsDesignedPlayerRange() {
        GameConfig config = new GameConfig(6, 10);

        assertEquals(6, config.minPlayers());
        assertEquals(10, config.maxPlayers());
    }

    @Test
    void rejectsNonPositiveMinimum() {
        assertThrows(InvalidConfigurationException.class, () -> new GameConfig(0, 10));
    }

    @Test
    void rejectsMaximumBelowMinimum() {
        assertThrows(InvalidConfigurationException.class, () -> new GameConfig(6, 5));
    }

    @Test
    void rejectsMaximumAboveDesignedLimit() {
        assertThrows(InvalidConfigurationException.class, () -> new GameConfig(6, 11));
    }
}
