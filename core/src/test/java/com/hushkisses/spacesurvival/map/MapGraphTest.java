package com.hushkisses.spacesurvival.map;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MapGraphTest {

    private static final SectorId ENGINEERING = new SectorId("engineering");
    private static final RoomId REACTOR = new RoomId("reactor");
    private static final RoomId POWER = new RoomId("power_control");

    @Test
    void registersLogicalMapAndQueriesAdjacencyFromEitherSide() {
        MapGraph graph = graphWithTwoRooms();

        graph.addConnection(new Connection(new ConnectionId("reactor_power"), REACTOR, POWER));

        assertEquals(Set.of(POWER), graph.adjacentRooms(REACTOR));
        assertEquals(Set.of(REACTOR), graph.adjacentRooms(POWER));
        assertEquals(1, graph.sectors().size());
        assertEquals(2, graph.rooms().size());
        assertEquals(1, graph.connections().size());
    }

    @Test
    void roomRequiresKnownSector() {
        MapGraph graph = new MapGraph();

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addRoom(new Room(REACTOR, ENGINEERING, "원자로실"))
        );
    }

    @Test
    void connectionRequiresKnownRooms() {
        MapGraph graph = new MapGraph();
        graph.addSector(new Sector(ENGINEERING, "기관 구역"));
        graph.addRoom(new Room(REACTOR, ENGINEERING, "원자로실"));

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addConnection(
                        new Connection(new ConnectionId("invalid"), REACTOR, POWER)
                )
        );
    }

    @Test
    void duplicateIdsAreRejected() {
        MapGraph graph = graphWithTwoRooms();

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addSector(new Sector(ENGINEERING, "중복"))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addRoom(new Room(REACTOR, ENGINEERING, "중복"))
        );

        Connection connection = new Connection(new ConnectionId("link"), REACTOR, POWER);
        graph.addConnection(connection);

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.addConnection(
                        new Connection(new ConnectionId("link"), POWER, REACTOR)
                )
        );
    }

    @Test
    void selfConnectionIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Connection(new ConnectionId("self"), REACTOR, REACTOR)
        );
    }

    @Test
    void unknownRoomAdjacencyQueryIsRejected() {
        MapGraph graph = graphWithTwoRooms();

        assertThrows(
                IllegalArgumentException.class,
                () -> graph.adjacentRooms(new RoomId("missing"))
        );
    }

    private static MapGraph graphWithTwoRooms() {
        MapGraph graph = new MapGraph();
        graph.addSector(new Sector(ENGINEERING, "기관 구역"));
        graph.addRoom(new Room(REACTOR, ENGINEERING, "원자로실"));
        graph.addRoom(new Room(POWER, ENGINEERING, "전력 제어실"));
        return graph;
    }
}
