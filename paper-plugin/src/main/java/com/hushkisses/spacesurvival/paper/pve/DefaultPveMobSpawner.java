package com.hushkisses.spacesurvival.paper.pve;

import com.hushkisses.spacesurvival.integration.mythicmobs.MythicMobsBridge;
import com.hushkisses.spacesurvival.integration.modelengine.ModelEngineBridge;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.Objects;

public final class DefaultPveMobSpawner implements PveMobSpawner {

    private static final String INFECTED_MOB_ID = "SpaceInfected";
    private static final String ALIEN_MOB_ID = "SpaceAlien";

    private final MythicMobsBridge mythicMobs;
    private final ModelEngineBridge modelEngine;

    public DefaultPveMobSpawner(
            MythicMobsBridge mythicMobs,
            ModelEngineBridge modelEngine
    ) {
        this.mythicMobs = Objects.requireNonNull(mythicMobs, "mythicMobs");
        this.modelEngine = Objects.requireNonNull(modelEngine, "modelEngine");
    }

    @Override
    public Entity spawnInfected(Location location) {
        Objects.requireNonNull(location, "location");
        return mythicMobs.spawn(INFECTED_MOB_ID, location, 1.0)
                .orElseGet(() -> location.getWorld().spawnEntity(location, EntityType.ZOMBIE));
    }

    @Override
    public Entity spawnAlien(Location location) {
        Objects.requireNonNull(location, "location");
        return mythicMobs.spawn(ALIEN_MOB_ID, location, 1.0)
                .orElseGet(() -> location.getWorld().spawnEntity(location, EntityType.SILVERFISH));
    }

    @Override
    public String backendStatus() {
        return "MythicMobs=" + mythicMobs.status()
                + ", ModelEngine=" + modelEngine.status();
    }
}
