package com.hushkisses.spacesurvival.social.pvp;

public record PvpPermissionContext(
        boolean emergencyDeclared,
        boolean attackerSecurityAuthorized,
        boolean targetConfirmedInfected,
        boolean scenarioAllows,
        boolean collapseStage,
        boolean specialEventAllows
) {
}
