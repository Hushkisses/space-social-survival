package com.hushkisses.spacesurvival.paper.pve;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface PveMobSpawner {
    Entity spawnInfected(Location location);
    Entity spawnAlien(Location location);
    String backendStatus();
}
