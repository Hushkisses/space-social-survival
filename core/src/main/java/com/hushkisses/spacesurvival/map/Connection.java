package com.hushkisses.spacesurvival.map;

import java.util.Objects;

public record Connection(ConnectionId id, RoomId first, RoomId second) {

    public Connection {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        if (first.equals(second)) {
            throw new IllegalArgumentException("Connection endpoints must be different rooms");
        }
    }

    public boolean contains(RoomId roomId) {
        Objects.requireNonNull(roomId, "roomId");
        return first.equals(roomId) || second.equals(roomId);
    }

    public RoomId other(RoomId roomId) {
        Objects.requireNonNull(roomId, "roomId");

        if (first.equals(roomId)) {
            return second;
        }
        if (second.equals(roomId)) {
            return first;
        }

        throw new IllegalArgumentException("Room " + roomId + " is not part of connection " + id);
    }
}
