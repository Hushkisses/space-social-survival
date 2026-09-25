package com.hushkisses.spacesurvival.communication;

public record CommunicationDecision(
        boolean allowed,
        CommunicationMode mode,
        CommunicationDenialReason denialReason
) {
    public static CommunicationDecision allow(CommunicationMode mode) {
        return new CommunicationDecision(true, mode, null);
    }

    public static CommunicationDecision deny(CommunicationDenialReason reason) {
        return new CommunicationDecision(false, null, reason);
    }
}
