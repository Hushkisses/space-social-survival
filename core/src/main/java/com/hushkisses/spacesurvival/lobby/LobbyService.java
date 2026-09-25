package com.hushkisses.spacesurvival.lobby;

import com.hushkisses.spacesurvival.config.GameConfig;
import com.hushkisses.spacesurvival.game.GamePhase;
import com.hushkisses.spacesurvival.game.GameSession;
import com.hushkisses.spacesurvival.game.GameSessionId;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.player.PlayerState;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LobbyService {

    private final GameConfig config;
    private final Clock clock;
    private final Map<PlayerId, PlayerState> players = new LinkedHashMap<>();
    private GameSession gameSession;

    public LobbyService(GameConfig config) {
        this(config, Clock.systemUTC());
    }

    public LobbyService(GameConfig config, Clock clock) {
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public LobbyJoinResult join(PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId");

        if (gameSession != null) {
            return LobbyJoinResult.MATCH_ALREADY_STARTED;
        }

        PlayerState existing = players.get(playerId);
        if (existing != null) {
            existing.reconnect();
            return LobbyJoinResult.ALREADY_JOINED;
        }

        if (players.size() >= config.maxPlayers()) {
            return LobbyJoinResult.FULL;
        }

        players.put(playerId, PlayerState.create(playerId, clock));
        return LobbyJoinResult.JOINED;
    }

    public LobbyLeaveResult leave(PlayerId playerId) {
        Objects.requireNonNull(playerId, "playerId");

        if (gameSession != null) {
            return LobbyLeaveResult.MATCH_ALREADY_STARTED;
        }

        return players.remove(playerId) == null
                ? LobbyLeaveResult.NOT_JOINED
                : LobbyLeaveResult.LEFT;
    }

    public void disconnect(PlayerId playerId) {
        PlayerState state = players.get(Objects.requireNonNull(playerId, "playerId"));
        if (state != null) {
            state.disconnect();
        }
    }

    public void reconnect(PlayerId playerId) {
        PlayerState state = players.get(Objects.requireNonNull(playerId, "playerId"));
        if (state != null) {
            state.reconnect();
        }
    }

    public boolean contains(PlayerId playerId) {
        return players.containsKey(Objects.requireNonNull(playerId, "playerId"));
    }

    public Optional<PlayerState> playerState(PlayerId playerId) {
        return Optional.ofNullable(players.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public List<PlayerId> alivePlayers() {
        return players.values().stream()
                .filter(PlayerState::isAlive)
                .map(PlayerState::id)
                .toList();
    }

    public GameSession start() {
        return startInternal(true);
    }

    public GameSession startForDevelopment() {
        return startInternal(false);
    }

    private GameSession startInternal(boolean enforceMinimumPlayers) {
        if (gameSession != null) {
            throw new LobbyStartException("Match has already started");
        }

        if (players.isEmpty()) {
            throw new LobbyStartException("No players in lobby");
        }

        if (enforceMinimumPlayers && players.size() < config.minPlayers()) {
            throw new LobbyStartException(
                    "Not enough players: " + players.size() + "/" + config.minPlayers()
            );
        }

        gameSession = GameSession.create(GameSessionId.random(), clock);
        gameSession.transitionTo(GamePhase.PREPARING);
        return gameSession;
    }

    public Optional<GameSession> gameSession() {
        return Optional.ofNullable(gameSession);
    }

    public void resetForNextMatch() {
        LinkedHashMap<PlayerId, PlayerState> refreshed = new LinkedHashMap<>();
        players.forEach((playerId, previous) -> {
            PlayerState next = PlayerState.create(playerId, clock);
            if (!previous.isConnected()) {
                next.disconnect();
            }
            refreshed.put(playerId, next);
        });
        players.clear();
        players.putAll(refreshed);
        gameSession = null;
    }

    public LobbySnapshot snapshot() {
        return new LobbySnapshot(
                config.minPlayers(),
                config.maxPlayers(),
                List.copyOf(players.keySet()),
                players.values().stream().filter(PlayerState::isConnected).count(),
                gameSession == null ? null : gameSession.phase()
        );
    }
}
