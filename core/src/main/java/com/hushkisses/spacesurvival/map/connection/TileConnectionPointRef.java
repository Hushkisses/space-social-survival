package com.hushkisses.spacesurvival.map.connection;

import com.hushkisses.spacesurvival.map.tile.ConnectionPointId;
import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.Objects;

public record TileConnectionPointRef(
        TileId tileId,
        ConnectionPointId pointId
) {

    public TileConnectionPointRef {
        Objects.requireNonNull(tileId, "tileId");
        Objects.requireNonNull(pointId, "pointId");
    }

    @Override
    public String toString() {
        return tileId + ":" + pointId;
    }
}
