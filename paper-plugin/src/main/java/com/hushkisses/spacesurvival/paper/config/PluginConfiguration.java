package com.hushkisses.spacesurvival.paper.config;

import com.hushkisses.spacesurvival.config.BalanceConfig;
import com.hushkisses.spacesurvival.config.GameConfig;

public record PluginConfiguration(
        GameConfig game,
        BalanceConfig balance,
        MatchSetupConfig matchSetup
) {
}
