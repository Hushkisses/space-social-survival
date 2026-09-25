package com.hushkisses.spacesurvival.social.sanction;

public record SanctionExecutionContext(
        boolean medicalAvailable,
        boolean securityAuthorized,
        boolean detentionAvailable,
        boolean airlockAvailable
) {
}
