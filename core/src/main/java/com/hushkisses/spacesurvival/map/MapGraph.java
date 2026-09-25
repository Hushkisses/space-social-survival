package com.hushkisses.spacesurvival.map;

import java.util.*;

public final class MapGraph {

    private final Map<SectorId, Sector> sectors = new LinkedHashMap<>();
    private final Map<RoomId, Room> rooms = new LinkedHashMap<>();
    private final Map<ConnectionId, Connection> connections = new LinkedHashMap<>();
    private final Map<RoomId, Set<ConnectionId>> connectionsByRoom = new HashMap<>();

    public void addSector(Sector sector) {
        Objects.requireNonNull(sector, "sector");
        if (sectors.putIfAbsent(sector.id(), sector) != null) {
            throw new IllegalArgumentException("Duplicate sector id: " + sector.id());
        }
    }

    public void addRoom(Room room) {
        Objects.requireNonNull(room, "room");

        if (!sectors.containsKey(room.sectorId())) {
            throw new IllegalArgumentException("Unknown sector id: " + room.sectorId());
        }
        if (rooms.putIfAbsent(room.id(), room) != null) {
            throw new IllegalArgumentException("Duplicate room id: " + room.id());
        }

        connectionsByRoom.put(room.id(), new LinkedHashSet<>());
    }

    public void addConnection(Connection connection) {
        Objects.requireNonNull(connection, "connection");

        if (!rooms.containsKey(connection.first())) {
            throw new IllegalArgumentException("Unknown room id: " + connection.first());
        }
        if (!rooms.containsKey(connection.second())) {
            throw new IllegalArgumentException("Unknown room id: " + connection.second());
        }
        if (connections.putIfAbsent(connection.id(), connection) != null) {
            throw new IllegalArgumentException("Duplicate connection id: " + connection.id());
        }

        connectionsByRoom.get(connection.first()).add(connection.id());
        connectionsByRoom.get(connection.second()).add(connection.id());
    }

    public Optional<Sector> sector(SectorId id) {
        return Optional.ofNullable(sectors.get(Objects.requireNonNull(id, "id")));
    }

    public Optional<Room> room(RoomId id) {
        return Optional.ofNullable(rooms.get(Objects.requireNonNull(id, "id")));
    }

    public Optional<Connection> connection(ConnectionId id) {
        return Optional.ofNullable(connections.get(Objects.requireNonNull(id, "id")));
    }

    public Set<RoomId> adjacentRooms(RoomId roomId) {
        Objects.requireNonNull(roomId, "roomId");

        if (!rooms.containsKey(roomId)) {
            throw new IllegalArgumentException("Unknown room id: " + roomId);
        }

        LinkedHashSet<RoomId> result = new LinkedHashSet<>();
        for (ConnectionId connectionId : connectionsByRoom.get(roomId)) {
            result.add(connections.get(connectionId).other(roomId));
        }
        return Collections.unmodifiableSet(result);
    }

    public Collection<Sector> sectors() {
        return Collections.unmodifiableCollection(sectors.values());
    }

    public Collection<Room> rooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    public Collection<Connection> connections() {
        return Collections.unmodifiableCollection(connections.values());
    }
}
