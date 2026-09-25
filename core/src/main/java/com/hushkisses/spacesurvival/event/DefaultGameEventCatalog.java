package com.hushkisses.spacesurvival.event;

public final class DefaultGameEventCatalog {

    private DefaultGameEventCatalog() {
    }

    public static GameEventRegistry createRegistry() {
        GameEventRegistry registry = new GameEventRegistry();
        DefaultSmallEventCatalog.create().forEach(registry::register);
        DefaultMajorEventCatalog.create().forEach(registry::register);
        return registry;
    }
}
