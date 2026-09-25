package com.hushkisses.spacesurvival.lobby;

import com.hushkisses.spacesurvival.config.GameConfig;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LobbyRematchTest {

    @Test
    void productionStartStillEnforcesMinimumPlayers() {
        LobbyService lobby = new LobbyService(new GameConfig(6, 10));
        lobby.join(PlayerId.of(UUID.randomUUID()));

        assertThrows(LobbyStartException.class, lobby::start);
    }

    @Test
    void developmentStartAllowsSinglePlayerWithoutChangingProductionRule() {
        LobbyService lobby = new LobbyService(new GameConfig(6, 10));
        PlayerId player = PlayerId.of(UUID.randomUUID());
        lobby.join(player);

        assertNotNull(lobby.startForDevelopment());
        assertTrue(lobby.gameSession().isPresent());

        lobby.resetForNextMatch();

        assertFalse(lobby.gameSession().isPresent());
        assertTrue(lobby.contains(player));
        assertTrue(lobby.playerState(player).orElseThrow().isAlive());
        assertThrows(LobbyStartException.class, lobby::start);
    }
}
