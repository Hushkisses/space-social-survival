package com.hushkisses.spacesurvival.map.connection;

public record ConnectionAccessContext(
        boolean powerAvailable,
        boolean keycardAvailable
) {

    public static ConnectionAccessContext none() {
        return new ConnectionAccessContext(false, false);
    }
}
