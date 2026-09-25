package com.hushkisses.spacesurvival.config;

public record GameConfig(
        int minPlayers,
        int maxPlayers
) {

    public GameConfig {
        if (minPlayers < 1) {
            throw new InvalidConfigurationException("minPlayers must be at least 1");
        }
        if (maxPlayers < minPlayers) {
            throw new InvalidConfigurationException("maxPlayers must be greater than or equal to minPlayers");
        }
        if (maxPlayers > 10) {
            throw new InvalidConfigurationException("maxPlayers must not exceed the designed maximum of 10");
        }
    }
}
