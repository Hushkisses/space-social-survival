package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.lobby.LobbyService;
import com.hushkisses.spacesurvival.paper.config.PluginConfiguration;
import com.hushkisses.spacesurvival.paper.config.PluginConfigurationLoader;
import com.hushkisses.spacesurvival.paper.lobby.LobbyConnectionListener;
import com.hushkisses.spacesurvival.paper.runtime.GameRuntimeService;
import com.hushkisses.spacesurvival.paper.ui.OpeningBriefingUi;
import com.hushkisses.spacesurvival.paper.ui.OpeningUiListener;
import com.hushkisses.spacesurvival.paper.ui.RoleSelectionUi;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.ship.ShipState;
import com.hushkisses.spacesurvival.role.RoleRegistry;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpaceSurvivalPlugin extends JavaPlugin {

    private PluginConfiguration configuration;
    private LobbyService lobbyService;
    private RoleRegistry roleRegistry;
    private RoleSelectionService roleSelectionService;
    private OpeningBriefingUi openingBriefingUi;
    private RoleSelectionUi roleSelectionUi;
    private GameRuntimeService gameRuntimeService;
    private ShipState shipState;

    @Override
    public void onEnable() {
        configuration = new PluginConfigurationLoader(this).load();
        lobbyService = new LobbyService(configuration.game());
        roleRegistry = DefaultRoleCatalog.createRegistry();
        roleSelectionService = new RoleSelectionService(roleRegistry);
        openingBriefingUi = new OpeningBriefingUi();
        roleSelectionUi = new RoleSelectionUi(this, roleSelectionService, roleRegistry);
        shipState = ShipState.healthy();
        gameRuntimeService = new GameRuntimeService(
                this,
                configuration.balance().targetMatchMinutes(),
                shipState
        );

        registerCommands();
        getServer().getPluginManager().registerEvents(
                new LobbyConnectionListener(lobbyService),
                this
        );
        getServer().getPluginManager().registerEvents(
                new OpeningUiListener(
                        openingBriefingUi,
                        roleSelectionUi,
                        roleSelectionService,
                        roleRegistry
                ),
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
        getLogger().info("Role registry loaded: " + roleRegistry.size() + " roles");
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

    public RoleRegistry roleRegistry() {
        if (roleRegistry == null) {
            throw new IllegalStateException("Role registry is not initialized yet");
        }
        return roleRegistry;
    }

    public RoleSelectionService roleSelectionService() {
        if (roleSelectionService == null) {
            throw new IllegalStateException("Role selection service is not initialized yet");
        }
        return roleSelectionService;
    }

    public OpeningBriefingUi openingBriefingUi() {
        if (openingBriefingUi == null) {
            throw new IllegalStateException("Opening briefing UI is not initialized yet");
        }
        return openingBriefingUi;
    }

    public RoleSelectionUi roleSelectionUi() {
        if (roleSelectionUi == null) {
            throw new IllegalStateException("Role selection UI is not initialized yet");
        }
        return roleSelectionUi;
    }

    public GameRuntimeService gameRuntimeService() {
        if (gameRuntimeService == null) {
            throw new IllegalStateException("Game runtime service is not initialized yet");
        }
        return gameRuntimeService;
    }

    public ShipState shipState() {
        if (shipState == null) {
            throw new IllegalStateException("Ship state is not initialized yet");
        }
        return shipState;
    }

    public void resetShipState() {
        shipState = ShipState.healthy();
        gameRuntimeService = new GameRuntimeService(
                this,
                configuration.balance().targetMatchMinutes(),
                shipState
        );
    }

    private void registerCommands() {
        PluginCommand command = getCommand("space");
        if (command == null) {
            throw new IllegalStateException("Command 'space' is missing from plugin.yml");
        }

        command.setExecutor(new SpaceCommand(this));
    }
}
