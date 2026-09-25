package com.hushkisses.spacesurvival.social.pvp;

import java.util.Objects;

public final class ConditionalPvpPolicy {

    public boolean isAllowed(PvpPermissionContext context) {
        Objects.requireNonNull(context, "context");

        if (context.collapseStage()) return true;
        if (context.scenarioAllows()) return true;
        if (context.specialEventAllows()) return true;
        if (context.targetConfirmedInfected()) return true;
        return context.emergencyDeclared() && context.attackerSecurityAuthorized();
    }
}
