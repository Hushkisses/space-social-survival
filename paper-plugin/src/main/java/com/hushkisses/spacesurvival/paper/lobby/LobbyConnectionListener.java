package com.hushkisses.spacesurvival.paper.lobby;

import com.hushkisses.spacesurvival.lobby.LobbyService;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public final class LobbyConnectionListener implements Listener {

    private final LobbyService lobbyService;

    public LobbyConnectionListener(LobbyService lobbyService) {
        this.lobbyService = Objects.requireNonNull(lobbyService, "lobbyService");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        lobbyService.reconnect(PlayerId.of(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lobbyService.disconnect(PlayerId.of(event.getPlayer().getUniqueId()));
    }
}
