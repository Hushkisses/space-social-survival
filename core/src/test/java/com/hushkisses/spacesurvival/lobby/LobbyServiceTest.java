package com.hushkisses.spacesurvival.lobby;

import com.hushkisses.spacesurvival.config.GameConfig;
import com.hushkisses.spacesurvival.game.GamePhase;
import com.hushkisses.spacesurvival.game.GameSession;
import com.hushkisses.spacesurvival.player.PlayerId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LobbyServiceTest {

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-09-25T00:00:00Z"),
            ZoneOffset.UTC
    );

    @Test
    void joinsUpToConfiguredCapacity() {
        LobbyService lobby = new LobbyService(new GameConfig(2, 3), clock);

        assertEquals(LobbyJoinResult.JOINED, lobby.join(player(1)));
        assertEquals(LobbyJoinResult.JOINED, lobby.join(player(2)));
        assertEquals(LobbyJoinResult.JOINED, lobby.join(player(3)));
        assertEquals(LobbyJoinResult.FULL, lobby.join(player(4)));

        assertEquals(3, lobby.snapshot().playerCount());
    }

    @Test
    void duplicateJoinIsIdempotentAndReconnects() {
        LobbyService lobby = new LobbyService(new GameConfig(1, 3), clock);
        PlayerId player = player(1);

        assertEquals(LobbyJoinResult.JOINED, lobby.join(player));
        lobby.disconnect(player);
        assertEquals(0, lobby.snapshot().connectedPlayers());

        assertEquals(LobbyJoinResult.ALREADY_JOINED, lobby.join(player));
        assertEquals(1, lobby.snapshot().connectedPlayers());
        assertEquals(1, lobby.snapshot().playerCount());
    }

    @Test
    void leaveWorksBeforeStart() {
        LobbyService lobby = new LobbyService(new GameConfig(1, 3), clock);
        PlayerId player = player(1);

        lobby.join(player);

        assertEquals(LobbyLeaveResult.LEFT, lobby.leave(player));
        assertEquals(LobbyLeaveResult.NOT_JOINED, lobby.leave(player));
        assertEquals(0, lobby.snapshot().playerCount());
    }

    @Test
    void startRequiresConfiguredMinimumPlayers() {
        LobbyService lobby = new LobbyService(new GameConfig(2, 3), clock);
        lobby.join(player(1));

        assertThrows(LobbyStartException.class, lobby::start);
    }

    @Test
    void startCreatesSessionInPreparingPhase() {
        LobbyService lobby = new LobbyService(new GameConfig(2, 3), clock);
        lobby.join(player(1));
        lobby.join(player(2));

        GameSession session = lobby.start();

        assertEquals(GamePhase.PREPARING, session.phase());
        assertTrue(lobby.snapshot().started());
        assertEquals(GamePhase.PREPARING, lobby.snapshot().gamePhase());
    }

    @Test
    void membershipIsFrozenAfterStart() {
        LobbyService lobby = new LobbyService(new GameConfig(1, 3), clock);
        PlayerId first = player(1);
        lobby.join(first);
        lobby.start();

        assertEquals(LobbyJoinResult.MATCH_ALREADY_STARTED, lobby.join(player(2)));
        assertEquals(LobbyLeaveResult.MATCH_ALREADY_STARTED, lobby.leave(first));
        assertThrows(LobbyStartException.class, lobby::start);
    }

    @Test
    void disconnectDoesNotRemoveLobbyState() {
        LobbyService lobby = new LobbyService(new GameConfig(1, 3), clock);
        PlayerId player = player(1);

        lobby.join(player);
        lobby.disconnect(player);

        assertTrue(lobby.contains(player));
        assertEquals(1, lobby.snapshot().playerCount());
        assertEquals(0, lobby.snapshot().connectedPlayers());

        lobby.reconnect(player);
        assertEquals(1, lobby.snapshot().connectedPlayers());
    }

    private static PlayerId player(int suffix) {
        return PlayerId.of(UUID.fromString(
                "00000000-0000-0000-0000-" + String.format("%012d", suffix)
        ));
    }
}
