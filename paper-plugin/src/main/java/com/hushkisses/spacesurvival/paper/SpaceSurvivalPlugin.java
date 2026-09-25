package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.lobby.LobbyService;
import com.hushkisses.spacesurvival.paper.config.PluginConfiguration;
import com.hushkisses.spacesurvival.paper.config.PluginConfigurationLoader;
import com.hushkisses.spacesurvival.paper.lobby.LobbyConnectionListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpaceSurvivalPlugin extends JavaPlugin {

    private PluginConfiguration configuration;
    private LobbyService lobbyService;

    @Override
    public void onEnable() {
        configuration = new PluginConfigurationLoader(this).load();
        lobbyService = new LobbyService(configuration.game());

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new LobbyConnectionListener(lobbyService),
                this
        );

        getLogger().info("SpaceSurvival enabled. core=" + BootstrapMarker.moduleName());
        getLogger().info(
                "Configuration loaded: players="
                        + configuration.game().minPlayers()
                        + "-"
                        + configuration.game().maxPlayers()
                        + ", targetMatchMinutes="
                        + configuration.balance().targetMatchMinutes()
                        + ", returnHoldSeconds="
                        + configuration.balance().returnHoldSeconds()
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("SpaceSurvival disabled.");
    }

    public PluginConfiguration configuration() {
        if (configuration == null) {
            throw new IllegalStateException("Plugin configuration is not loaded yet");
        }
        return configuration;
    }

    public LobbyService lobbyService() {
        if (lobbyService == null) {
            throw new IllegalStateException("Lobby service is not initialized yet");
        }
        return lobbyService;
    }

    private void registerCommands() {
        PluginCommand command = getCommand("space");
        if (command == null) {
            throw new IllegalStateException("Command 'space' is missing from plugin.yml");
        }

        command.setExecutor(new SpaceCommand(this));
    }
}
