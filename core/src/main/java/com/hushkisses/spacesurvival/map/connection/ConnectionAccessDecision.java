package com.hushkisses.spacesurvival.map.connection;

import java.util.Objects;

public record ConnectionAccessDecision(
        boolean allowed,
        DenialReason denialReason
) {

    public ConnectionAccessDecision {
        if (allowed && denialReason != null) {
            throw new IllegalArgumentException("Allowed decision cannot have a denial reason");
        }
        if (!allowed) {
            Objects.requireNonNull(denialReason, "denialReason");
        }
    }

    public static ConnectionAccessDecision allow() {
        return new ConnectionAccessDecision(true, null);
    }

    public static ConnectionAccessDecision deny(DenialReason reason) {
        return new ConnectionAccessDecision(false, Objects.requireNonNull(reason, "reason"));
    }

    public enum DenialReason {
        LOCKED,
        POWER_REQUIRED,
        KEYCARD_REQUIRED,
        DISABLED
    }
}
