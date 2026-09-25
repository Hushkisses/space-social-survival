package com.hushkisses.spacesurvival.map.connection;

import java.util.Objects;

public final class ConnectionAccessPolicy {

    public ConnectionAccessDecision evaluate(
            ConnectionState state,
            ConnectionAccessContext context
    ) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(context, "context");

        return switch (state) {
            case OPEN -> ConnectionAccessDecision.allow();
            case LOCKED -> ConnectionAccessDecision.deny(
                    ConnectionAccessDecision.DenialReason.LOCKED
            );
            case POWER_REQUIRED -> context.powerAvailable()
                    ? ConnectionAccessDecision.allow()
                    : ConnectionAccessDecision.deny(
                            ConnectionAccessDecision.DenialReason.POWER_REQUIRED
                    );
            case KEYCARD_REQUIRED -> context.keycardAvailable()
                    ? ConnectionAccessDecision.allow()
                    : ConnectionAccessDecision.deny(
                            ConnectionAccessDecision.DenialReason.KEYCARD_REQUIRED
                    );
            case DISABLED -> ConnectionAccessDecision.deny(
                    ConnectionAccessDecision.DenialReason.DISABLED
            );
        };
    }
}
