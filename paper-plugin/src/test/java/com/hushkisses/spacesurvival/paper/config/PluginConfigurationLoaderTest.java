package com.hushkisses.spacesurvival.paper.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginConfigurationLoaderTest {

    @Test
    void legacyBalanceFileReceivesNewDefaultsWithoutOverwritingExistingValues() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("match.target-minutes", 50);
        yaml.set("return.hold-seconds", 300);

        assertTrue(PluginConfigurationLoader.applyBalanceDefaults(yaml));

        assertEquals(50, yaml.getInt("match.target-minutes"));
        assertEquals(300, yaml.getInt("return.hold-seconds"));

        assertEquals(10, yaml.getInt("map.min-tiles"));
        assertEquals(14, yaml.getInt("map.max-tiles"));
        assertEquals(6, yaml.getInt("map.max-dead-ends"));
        assertEquals(6, yaml.getInt("map.max-core-distance"));
        assertEquals(500, yaml.getInt("map.max-attempts"));
        assertEquals(50, yaml.getInt("objectives.secret-mission-chance-percent"));
        assertEquals(2, yaml.getInt("events.initial-small-min"));
        assertEquals(3, yaml.getInt("events.initial-small-max"));
        assertEquals(45, yaml.getInt("scenario.accident-weight"));
        assertEquals(35, yaml.getInt("scenario.sabotage-weight"));
        assertEquals(20, yaml.getInt("scenario.infection-weight"));
    }

    @Test
    void completeBalanceFileDoesNotNeedMigration() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("match.target-minutes", 45);
        yaml.set("return.hold-seconds", 240);
        yaml.set("map.min-tiles", 10);
        yaml.set("map.max-tiles", 14);
        yaml.set("map.max-dead-ends", 6);
        yaml.set("map.max-core-distance", 6);
        yaml.set("map.max-attempts", 500);
        yaml.set("objectives.secret-mission-chance-percent", 50);
        yaml.set("events.initial-small-min", 2);
        yaml.set("events.initial-small-max", 3);
        yaml.set("scenario.accident-weight", 45);
        yaml.set("scenario.sabotage-weight", 35);
        yaml.set("scenario.infection-weight", 20);

        assertFalse(PluginConfigurationLoader.applyBalanceDefaults(yaml));
    }
}
