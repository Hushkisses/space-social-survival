package com.hushkisses.spacesurvival.config;

public record BalanceConfig(
        int targetMatchMinutes,
        int returnHoldSeconds
) {

    public BalanceConfig {
        if (targetMatchMinutes < 1) {
            throw new InvalidConfigurationException("targetMatchMinutes must be positive");
        }
        if (returnHoldSeconds < 1) {
            throw new InvalidConfigurationException("returnHoldSeconds must be positive");
        }
    }
}
