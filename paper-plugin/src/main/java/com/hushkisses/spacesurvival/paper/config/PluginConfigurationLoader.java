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
        YamlConfiguration balanceYaml = YamlConfiguration.loadConfiguration(balanceFile);

        BalanceConfig balance = new BalanceConfig(
                balanceYaml.getInt("match.target-minutes"),
                balanceYaml.getInt("return.hold-seconds")
        );

        return new PluginConfiguration(game, balance);
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }
}
