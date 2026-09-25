package com.hushkisses.spacesurvival.death;

import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.player.PlayerState;

import java.time.Clock;
import java.util.*;

public final class DeathService {

    private final Clock clock;
    private final Map<PlayerId, DeathRecord> records = new LinkedHashMap<>();

    public DeathService() {
        this(Clock.systemUTC());
    }

    public DeathService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public DeathRecord registerDeath(
            PlayerState playerState,
            DeathCause cause,
            boolean infectedAtDeath
    ) {
        Objects.requireNonNull(playerState, "playerState");
        Objects.requireNonNull(cause, "cause");

        DeathRecord existing = records.get(playerState.id());
        if (existing != null) {
            return existing;
        }

        playerState.markDead();
        DeathRecord record = new DeathRecord(
                playerState.id(),
                cause,
                clock.instant(),
                infectedAtDeath
        );
        records.put(playerState.id(), record);
        return record;
    }

    public Optional<DeathRecord> record(PlayerId playerId) {
        return Optional.ofNullable(records.get(Objects.requireNonNull(playerId, "playerId")));
    }

    public boolean isDead(PlayerId playerId) {
        return records.containsKey(Objects.requireNonNull(playerId, "playerId"));
    }

    public Collection<DeathRecord> records() {
        return Collections.unmodifiableCollection(records.values());
    }

    public void clear() {
        records.clear();
    }
}
