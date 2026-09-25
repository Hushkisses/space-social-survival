package com.hushkisses.spacesurvival.map.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionAccessPolicyTest {

    private final ConnectionAccessPolicy policy = new ConnectionAccessPolicy();

    @Test
    void openIsAlwaysAllowed() {
        assertTrue(policy.evaluate(ConnectionState.OPEN, ConnectionAccessContext.none()).allowed());
    }

    @Test
    void lockedIsDenied() {
        ConnectionAccessDecision decision =
                policy.evaluate(ConnectionState.LOCKED, new ConnectionAccessContext(true, true));

        assertFalse(decision.allowed());
        assertEquals(ConnectionAccessDecision.DenialReason.LOCKED, decision.denialReason());
    }

    @Test
    void powerRequiredNeedsPower() {
        assertFalse(policy.evaluate(
                ConnectionState.POWER_REQUIRED,
                new ConnectionAccessContext(false, true)
        ).allowed());

        assertTrue(policy.evaluate(
                ConnectionState.POWER_REQUIRED,
                new ConnectionAccessContext(true, false)
        ).allowed());
    }

    @Test
    void keycardRequiredNeedsKeycard() {
        assertFalse(policy.evaluate(
                ConnectionState.KEYCARD_REQUIRED,
                new ConnectionAccessContext(true, false)
        ).allowed());

        assertTrue(policy.evaluate(
                ConnectionState.KEYCARD_REQUIRED,
                new ConnectionAccessContext(false, true)
        ).allowed());
    }

    @Test
    void disabledIsAlwaysDenied() {
        ConnectionAccessDecision decision =
                policy.evaluate(ConnectionState.DISABLED, new ConnectionAccessContext(true, true));

        assertFalse(decision.allowed());
        assertEquals(ConnectionAccessDecision.DenialReason.DISABLED, decision.denialReason());
    }
}
