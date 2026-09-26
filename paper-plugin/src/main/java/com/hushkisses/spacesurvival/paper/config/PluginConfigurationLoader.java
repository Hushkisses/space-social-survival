package com.hushkisses.spacesurvival.paper.config;

import com.hushkisses.spacesurvival.config.BalanceConfig;
import com.hushkisses.spacesurvival.config.GameConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PluginConfigurationLoader {

    private static final Map<String, Integer> BALANCE_DEFAULTS = balanceDefaults();

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

        if (applyBalanceDefaults(yaml)) {
            saveMigratedBalance(yaml, balanceFile);
            plugin.getLogger().info("balance.yml migrated with newly introduced default settings.");
        }

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
                yaml.getInt("starting-ship.power"),
                yaml.getInt("starting-ship.oxygen"),
                yaml.getInt("starting-ship.hull"),
                yaml.getInt("starting-ship.reactor"),
                yaml.getInt("starting-shared-resources.repair-parts"),
                yaml.getInt("starting-shared-resources.power-cells"),
                yaml.getInt("starting-shared-resources.fuel"),
                yaml.getInt("starting-shared-resources.medical-supplies"),
                yaml.getInt("events.initial-small-min"),
                yaml.getInt("events.initial-small-max"),
                yaml.getInt("scenario.accident-weight"),
                yaml.getInt("scenario.sabotage-weight"),
                yaml.getInt("scenario.infection-weight"),
                yaml.getInt("incident.small-min-seconds"),
                yaml.getInt("incident.small-max-seconds"),
                yaml.getInt("incident.major-first-min-seconds"),
                yaml.getInt("incident.major-first-max-seconds"),
                yaml.getInt("incident.major-second-min-seconds"),
                yaml.getInt("incident.major-second-max-seconds")
        );

        return new PluginConfiguration(game, balance, matchSetup);
    }

    static boolean applyBalanceDefaults(YamlConfiguration yaml) {
        Objects.requireNonNull(yaml, "yaml");

        boolean changed = false;
        for (Map.Entry<String, Integer> entry : BALANCE_DEFAULTS.entrySet()) {
            if (!yaml.contains(entry.getKey())) {
                yaml.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private void saveMigratedBalance(YamlConfiguration yaml, File balanceFile) {
        try {
            yaml.save(balanceFile);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not save migrated balance.yml",
                    exception
            );
        }
    }

    private void saveResourceIfMissing(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
    }

    private static Map<String, Integer> balanceDefaults() {
        LinkedHashMap<String, Integer> defaults = new LinkedHashMap<>();
        defaults.put("match.target-minutes", 45);
        defaults.put("return.hold-seconds", 240);
        defaults.put("map.min-tiles", 10);
        defaults.put("map.max-tiles", 14);
        defaults.put("map.max-dead-ends", 6);
        defaults.put("map.max-core-distance", 6);
        defaults.put("map.max-attempts", 500);
        defaults.put("objectives.secret-mission-chance-percent", 50);
        // Playability-phase defaults: intentionally damaged enough to require
        // real recovery work before the first return-stage transition.
        defaults.put("starting-ship.power", 35);
        defaults.put("starting-ship.oxygen", 40);
        defaults.put("starting-ship.hull", 38);
        defaults.put("starting-ship.reactor", 30);
        // Small shared emergency reserve prevents an unlucky initial event roll
        // from making the mandatory opening recovery impossible.
        defaults.put("starting-shared-resources.repair-parts", 4);
        defaults.put("starting-shared-resources.power-cells", 2);
        defaults.put("starting-shared-resources.fuel", 2);
        defaults.put("starting-shared-resources.medical-supplies", 2);
        defaults.put("events.initial-small-min", 2);
        defaults.put("events.initial-small-max", 3);
        defaults.put("scenario.accident-weight", 45);
        defaults.put("scenario.sabotage-weight", 35);
        defaults.put("scenario.infection-weight", 20);
        defaults.put("incident.small-min-seconds", 180);
        defaults.put("incident.small-max-seconds", 360);
        defaults.put("incident.major-first-min-seconds", 1080);
        defaults.put("incident.major-first-max-seconds", 1440);
        defaults.put("incident.major-second-min-seconds", 1920);
        defaults.put("incident.major-second-max-seconds", 2280);
        return Map.copyOf(defaults);
    }
}
