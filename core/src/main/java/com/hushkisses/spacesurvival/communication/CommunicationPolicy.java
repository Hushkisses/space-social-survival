package com.hushkisses.spacesurvival.communication;

public final class CommunicationPolicy {

    public CommunicationDecision evaluate(
            boolean senderAlive,
            boolean recipientAlive,
            boolean withinProximity,
            boolean senderHasRadio,
            boolean recipientHasRadio,
            boolean longRangeAvailable
    ) {
        if (!senderAlive || !recipientAlive) {
            if (!senderAlive && !recipientAlive) {
                return CommunicationDecision.allow(CommunicationMode.DEAD_ONLY);
            }
            return CommunicationDecision.deny(
                    CommunicationDenialReason.DEAD_TO_LIVING_BLOCKED
            );
        }

        if (withinProximity) {
            return CommunicationDecision.allow(CommunicationMode.PROXIMITY);
        }

        if (!senderHasRadio || !recipientHasRadio) {
            return CommunicationDecision.deny(CommunicationDenialReason.RADIO_REQUIRED);
        }

        if (!longRangeAvailable) {
            return CommunicationDecision.deny(CommunicationDenialReason.RADIO_UNAVAILABLE);
        }

        return CommunicationDecision.allow(CommunicationMode.RADIO);
    }
}
