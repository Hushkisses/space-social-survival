package com.hushkisses.spacesurvival.communication;

import com.hushkisses.spacesurvival.player.PlayerId;

import java.util.Objects;

public final class RadioService {

    private final RadioRuntimeState state;
    private final CommunicationPolicy policy;

    public RadioService(RadioRuntimeState state, CommunicationPolicy policy) {
        this.state = Objects.requireNonNull(state, "state");
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public CommunicationDecision evaluate(
            PlayerId sender,
            boolean senderAlive,
            PlayerId recipient,
            boolean recipientAlive,
            boolean withinProximity
    ) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(recipient, "recipient");

        return policy.evaluate(
                senderAlive,
                recipientAlive,
                withinProximity,
                state.hasRadio(sender),
                state.hasRadio(recipient),
                state.longRangeAvailable()
        );
    }

    public RadioRuntimeState state() {
        return state;
    }
}
