package com.hushkisses.spacesurvival.lobby;

import com.hushkisses.spacesurvival.game.GamePhase;
import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.List;

public record LobbySnapshot(
        int minPlayers,
        int maxPlayers,
        List<PlayerId> players,
        long connectedPlayers,
        GamePhase gamePhase
) {

    public int playerCount() {
        return players.size();
    }

    public boolean started() {
        return gamePhase != null;
    }
}
