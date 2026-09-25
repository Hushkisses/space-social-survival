package com.hushkisses.spacesurvival.integration.voicechat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class SimpleVoiceChatBridge {

    public boolean isPluginPresent() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("voicechat");
        if (plugin == null) {
            plugin = Bukkit.getPluginManager().getPlugin("SimpleVoiceChat");
        }
        return plugin != null && plugin.isEnabled();
    }

    public boolean isApiPresent() {
        try {
            Class.forName("de.maxhenkel.voicechat.api.BukkitVoicechatService");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public boolean isAvailable() {
        return isPluginPresent() && isApiPresent();
    }

    public String status() {
        if (isAvailable()) {
            return "CONNECTED";
        }
        if (isPluginPresent()) {
            return "PLUGIN_PRESENT_API_UNAVAILABLE";
        }
        return "NOT_INSTALLED";
    }
}
