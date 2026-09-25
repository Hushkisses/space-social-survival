package com.hushkisses.spacesurvival.integration.modelengine;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class ModelEngineBridge {

    public boolean isPluginPresent() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ModelEngine");
        return plugin != null && plugin.isEnabled();
    }

    public boolean isApiPresent() {
        try {
            Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
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
}
