package com.hushkisses.spacesurvival.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BootstrapMarkerTest {

    @Test
    void exposesCoreModuleName() {
        assertEquals("space-survival-core", BootstrapMarker.moduleName());
    }
}
