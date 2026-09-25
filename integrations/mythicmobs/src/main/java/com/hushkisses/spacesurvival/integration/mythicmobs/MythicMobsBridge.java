package com.hushkisses.spacesurvival.integration.mythicmobs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Optional;

public final class MythicMobsBridge {

    public boolean isPluginPresent() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
        return plugin != null && plugin.isEnabled();
    }

    public boolean isApiPresent() {
        try {
            Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public boolean isAvailable() {
        return isPluginPresent() && isApiPresent();
    }

    public String status() {
        if (isAvailable()) return "CONNECTED";
        if (isPluginPresent()) return "PLUGIN_PRESENT_API_UNAVAILABLE";
        return "NOT_INSTALLED";
    }

    public Optional<Entity> spawn(String mythicMobId, Location location, double level) {
        if (mythicMobId == null || mythicMobId.isBlank()) {
            throw new IllegalArgumentException("mythicMobId must not be blank");
        }
        if (location == null) {
            throw new IllegalArgumentException("location");
        }
        if (level <= 0.0) {
            throw new IllegalArgumentException("level must be positive");
        }

        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object mythicBukkit = mythicBukkitClass.getMethod("inst").invoke(null);
            Object mobManager = mythicBukkitClass.getMethod("getMobManager").invoke(mythicBukkit);

            Object optionalMob = mobManager.getClass()
                    .getMethod("getMythicMob", String.class)
                    .invoke(mobManager, mythicMobId);

            if (!(optionalMob instanceof Optional<?> optional) || optional.isEmpty()) {
                return Optional.empty();
            }

            Object mythicMob = optional.get();

            Class<?> adapterClass = Class.forName("io.lumine.mythic.bukkit.BukkitAdapter");
            Method adaptMethod = adapterClass.getMethod("adapt", Location.class);
            Object abstractLocation = adaptMethod.invoke(null, location);

            Method spawnMethod = null;
            for (Method method : mythicMob.getClass().getMethods()) {
                if (method.getName().equals("spawn") && method.getParameterCount() == 2) {
                    spawnMethod = method;
                    break;
                }
            }
            if (spawnMethod == null) {
                return Optional.empty();
            }

            Object activeMob = spawnMethod.invoke(mythicMob, abstractLocation, level);
            if (activeMob == null) {
                return Optional.empty();
            }

            Object abstractEntity = activeMob.getClass().getMethod("getEntity").invoke(activeMob);
            Object bukkitEntity = abstractEntity.getClass().getMethod("getBukkitEntity").invoke(abstractEntity);

            return bukkitEntity instanceof Entity entity
                    ? Optional.of(entity)
                    : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError exception) {
            return Optional.empty();
        }
    }
}
