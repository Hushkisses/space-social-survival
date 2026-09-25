package com.hushkisses.spacesurvival.core;

/**
 * Minimal core-domain marker used by DEV-001 to verify the pure Java module.
 */
public final class BootstrapMarker {

    private BootstrapMarker() {
    }

    public static String moduleName() {
        return "space-survival-core";
    }
}
