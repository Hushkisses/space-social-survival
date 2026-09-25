package com.hushkisses.spacesurvival.map.connection;

import com.hushkisses.spacesurvival.map.tile.ConnectionPointId;
import com.hushkisses.spacesurvival.map.tile.TileId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionRouteTest {

    @Test
    void resolvesOtherEndpointFromEitherSide() {
        TileConnectionPointRef first = ref("medical", "north");
        TileConnectionPointRef second = ref("cargo", "south");
        ConnectionRoute route = new ConnectionRoute(first, second, ConnectionState.OPEN);

        assertEquals(second, route.other(first));
        assertEquals(first, route.other(second));
    }

    @Test
    void selfRouteIsRejected() {
        TileConnectionPointRef endpoint = ref("medical", "north");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ConnectionRoute(endpoint, endpoint, ConnectionState.OPEN)
        );
    }

    @Test
    void unrelatedEndpointIsRejected() {
        ConnectionRoute route = new ConnectionRoute(
                ref("medical", "north"),
                ref("cargo", "south"),
                ConnectionState.OPEN
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> route.other(ref("bridge", "west"))
        );
    }

    private static TileConnectionPointRef ref(String tile, String point) {
        return new TileConnectionPointRef(
                new TileId(tile),
                new ConnectionPointId(point)
        );
    }
}
