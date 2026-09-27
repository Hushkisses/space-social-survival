package com.hushkisses.spacesurvival.paper.config;

import com.hushkisses.spacesurvival.ending.ReturnRequirements;
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
        assertEquals(35, yaml.getInt("starting-ship.power"));
        assertEquals(40, yaml.getInt("starting-ship.oxygen"));
        assertEquals(38, yaml.getInt("starting-ship.hull"));
        assertEquals(30, yaml.getInt("starting-ship.reactor"));
        assertEquals(4, yaml.getInt("starting-shared-resources.repair-parts"));
        assertEquals(2, yaml.getInt("starting-shared-resources.power-cells"));
        assertEquals(2, yaml.getInt("starting-shared-resources.fuel"));
        assertEquals(2, yaml.getInt("starting-shared-resources.medical-supplies"));

        ReturnRequirements requirements = ReturnRequirements.developmentDefaults();
        boolean immediatelyReturnReady =
                yaml.getInt("starting-ship.power") >= requirements.minPower()
                        && yaml.getInt("starting-ship.oxygen") >= requirements.minOxygen()
                        && yaml.getInt("starting-ship.hull") >= requirements.minHull()
                        && yaml.getInt("starting-ship.reactor") >= requirements.minReactor();
        assertFalse(immediatelyReturnReady, "default opening state must require recovery work");

        assertEquals(2, yaml.getInt("events.initial-small-min"));
        assertEquals(3, yaml.getInt("events.initial-small-max"));
        assertEquals(45, yaml.getInt("scenario.accident-weight"));
        assertEquals(35, yaml.getInt("scenario.sabotage-weight"));
        assertEquals(20, yaml.getInt("scenario.infection-weight"));
        assertEquals(180, yaml.getInt("incident.small-min-seconds"));
        assertEquals(360, yaml.getInt("incident.small-max-seconds"));
        assertEquals(1080, yaml.getInt("incident.major-first-min-seconds"));
        assertEquals(1440, yaml.getInt("incident.major-first-max-seconds"));
        assertEquals(1920, yaml.getInt("incident.major-second-min-seconds"));
        assertEquals(2280, yaml.getInt("incident.major-second-max-seconds"));
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
        yaml.set("starting-ship.power", 35);
        yaml.set("starting-ship.oxygen", 40);
        yaml.set("starting-ship.hull", 38);
        yaml.set("starting-ship.reactor", 30);
        yaml.set("starting-shared-resources.repair-parts", 4);
        yaml.set("starting-shared-resources.power-cells", 2);
        yaml.set("starting-shared-resources.fuel", 2);
        yaml.set("starting-shared-resources.medical-supplies", 2);
        yaml.set("events.initial-small-min", 2);
        yaml.set("events.initial-small-max", 3);
        yaml.set("scenario.accident-weight", 45);
        yaml.set("scenario.sabotage-weight", 35);
        yaml.set("scenario.infection-weight", 20);
        yaml.set("incident.small-min-seconds", 180);
        yaml.set("incident.small-max-seconds", 360);
        yaml.set("incident.major-first-min-seconds", 1080);
        yaml.set("incident.major-first-max-seconds", 1440);
        yaml.set("incident.major-second-min-seconds", 1920);
        yaml.set("incident.major-second-max-seconds", 2280);

        assertFalse(PluginConfigurationLoader.applyBalanceDefaults(yaml));
    }
}
