package com.hushkisses.spacesurvival.player;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public final class PlayerState {

    private final PlayerId id;
    private final Clock clock;
    private final Instant joinedAt;

    private boolean alive;
    private boolean connected;
    private Instant connectionChangedAt;
    private Instant diedAt;

    private PlayerState(PlayerId id, Clock clock) {
        this.id = Objects.requireNonNull(id, "id");
        this.clock = Objects.requireNonNull(clock, "clock");

        Instant now = clock.instant();
        this.joinedAt = now;
        this.alive = true;
        this.connected = true;
        this.connectionChangedAt = now;
    }

    public static PlayerState create(PlayerId id) {
        return new PlayerState(id, Clock.systemUTC());
    }

    public static PlayerState create(PlayerId id, Clock clock) {
        return new PlayerState(id, clock);
    }

    public PlayerId id() {
        return id;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isConnected() {
        return connected;
    }

    public Instant joinedAt() {
        return joinedAt;
    }

    public Instant connectionChangedAt() {
        return connectionChangedAt;
    }

    public Instant diedAt() {
        return diedAt;
    }

    public void disconnect() {
        if (!connected) {
            return;
        }

        connected = false;
        connectionChangedAt = clock.instant();
    }

    public void reconnect() {
        if (connected) {
            return;
        }

        connected = true;
        connectionChangedAt = clock.instant();
    }

    public void markDead() {
        if (!alive) {
            return;
        }

        alive = false;
        diedAt = clock.instant();
    }
}
