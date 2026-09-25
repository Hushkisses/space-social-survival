package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpaceSurvivalPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        registerCommands();
        getLogger().info("SpaceSurvival enabled. core=" + BootstrapMarker.moduleName());
    }

    @Override
    public void onDisable() {
        getLogger().info("SpaceSurvival disabled.");
    }

    private void registerCommands() {
        PluginCommand command = getCommand("space");
        if (command == null) {
            throw new IllegalStateException("Command 'space' is missing from plugin.yml");
        }

        command.setExecutor(new SpaceCommand(this));
    }
}
