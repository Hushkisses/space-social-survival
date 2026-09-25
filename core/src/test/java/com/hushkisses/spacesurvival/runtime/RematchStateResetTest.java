package com.hushkisses.spacesurvival.runtime;

import com.hushkisses.spacesurvival.communication.RadioRuntimeState;
import com.hushkisses.spacesurvival.death.DeathCause;
import com.hushkisses.spacesurvival.death.DeathService;
import com.hushkisses.spacesurvival.event.GameEventId;
import com.hushkisses.spacesurvival.event.GameEventRuntimeState;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.player.PlayerState;
import com.hushkisses.spacesurvival.resource.ResourceLedger;
import com.hushkisses.spacesurvival.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RematchStateResetTest {

    @Test
    void mutableMatchStoresCanReturnToCleanState() {
        PlayerId player = PlayerId.of(UUID.randomUUID());

        ResourceLedger resources = new ResourceLedger();
        resources.shared().add(ResourceType.FUEL, 3);
        resources.personal(player).add(ResourceType.DATA_CORES, 1);
        resources.clear();
        assertTrue(resources.shared().snapshot().isEmpty());
        assertTrue(resources.personalSnapshot().isEmpty());

        RadioRuntimeState radio = new RadioRuntimeState();
        radio.setRadio(player, true);
        radio.setCommunicationsOutage(true);
        radio.reset();
        assertFalse(radio.hasRadio(player));
        assertTrue(radio.longRangeAvailable());

        GameEventRuntimeState events = new GameEventRuntimeState();
        events.setFlag("test", true);
        events.record(new GameEventId("test"), Instant.now());
        events.clear();
        assertTrue(events.activeFlags().isEmpty());
        assertTrue(events.history().isEmpty());

        DeathService deaths = new DeathService();
        PlayerState playerState = PlayerState.create(player);
        deaths.registerDeath(playerState, DeathCause.SCRIPTED, false);
        assertTrue(deaths.isDead(player));
        deaths.clear();
        assertFalse(deaths.isDead(player));
    }
}
