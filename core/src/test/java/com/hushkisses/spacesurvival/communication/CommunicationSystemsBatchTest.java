package com.hushkisses.spacesurvival.communication;

import com.hushkisses.spacesurvival.player.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CommunicationSystemsBatchTest {

    @Test
    void livingPlayersUseProximityOrRadio() {
        CommunicationPolicy policy = new CommunicationPolicy();

        assertEquals(
                CommunicationMode.PROXIMITY,
                policy.evaluate(true, true, true, false, false, true).mode()
        );
        assertEquals(
                CommunicationDenialReason.RADIO_REQUIRED,
                policy.evaluate(true, true, false, false, false, true).denialReason()
        );
        assertEquals(
                CommunicationMode.RADIO,
                policy.evaluate(true, true, false, true, true, true).mode()
        );
    }

    @Test
    void communicationOutageBlocksRadio() {
        PlayerId a = PlayerId.of(UUID.randomUUID());
        PlayerId b = PlayerId.of(UUID.randomUUID());
        RadioRuntimeState state = new RadioRuntimeState();
        state.setRadio(a, true);
        state.setRadio(b, true);

        RadioService radio = new RadioService(state, new CommunicationPolicy());
        assertTrue(radio.evaluate(a, true, b, true, false).allowed());

        state.setCommunicationsOutage(true);
        CommunicationDecision blocked = radio.evaluate(a, true, b, true, false);

        assertFalse(blocked.allowed());
        assertEquals(CommunicationDenialReason.RADIO_UNAVAILABLE, blocked.denialReason());
    }

    @Test
    void deadPlayersCannotTalkToLivingButCanTalkToDead() {
        CommunicationPolicy policy = new CommunicationPolicy();

        CommunicationDecision living = policy.evaluate(false, true, true, true, true, true);
        assertFalse(living.allowed());
        assertEquals(CommunicationDenialReason.DEAD_TO_LIVING_BLOCKED, living.denialReason());

        CommunicationDecision dead = policy.evaluate(false, false, false, false, false, false);
        assertTrue(dead.allowed());
        assertEquals(CommunicationMode.DEAD_ONLY, dead.mode());
    }
}
