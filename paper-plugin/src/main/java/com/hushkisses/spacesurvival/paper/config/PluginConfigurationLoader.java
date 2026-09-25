package com.hushkisses.spacesurvival.paper.config;

import com.hushkisses.spacesurvival.config.BalanceConfig;
import com.hushkisses.spacesurvival.config.GameConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class PluginConfigurationLoader {

    private final JavaPlugin plugin;

    public PluginConfigurationLoader(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public PluginConfiguration load() {
        plugin.saveDefaultConfig();
        saveResourceIfMissing("balance.yml");

        GameConfig game = new GameConfig(
                plugin.getConfig().getInt("game.min-players"),
                plugin.getConfig().getInt("game.max-players")
        );

        File balanceFile = new File(plugin.getDataFolder(), "balance.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(balanceFile);

        BalanceConfig balance = new BalanceConfig(
                yaml.getInt("match.target-minutes"),
                yaml.getInt("return.hold-seconds")
        );

        MatchSetupConfig matchSetup = new MatchSetupConfig(
                yaml.getInt("map.min-tiles"),
                yaml.getInt("map.max-tiles"),
                yaml.getInt("map.max-dead-ends"),
                yaml.getInt("map.max-core-distance"),
                yaml.getInt("map.max-attempts"),
                yaml.getInt("objectives.secret-mission-chance-percent"),
                yaml.getInt("events.initial-small-min"),
                yaml.getInt("events.initial-small-max"),
                yaml.getInt("scenario.accident-weight"),
                yaml.getInt("scenario.sabotage-weight"),
                yaml.getInt("scenario.infection-weight")
        );

        return new PluginConfiguration(game, balance, matchSetup);
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }
}
