package com.hushkisses.spacesurvival.paper.map;

import com.hushkisses.spacesurvival.map.tile.ConnectionPointDefinition;
import com.hushkisses.spacesurvival.map.tile.ConnectionPointId;
import com.hushkisses.spacesurvival.map.tile.ConnectionPointType;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;

import java.util.ArrayList;
import java.util.List;

public final class DebugTilePool {

    private DebugTilePool() {
    }

    public static List<TileDefinition> create() {
        ArrayList<TileDefinition> tiles = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            tiles.add(tile("core_" + i, TileCategory.CORE, 4));
        }
        for (int i = 1; i <= 5; i++) {
            tiles.add(tile("corridor_" + i, TileCategory.CORRIDOR, 3));
        }
        for (int i = 1; i <= 3; i++) {
            tiles.add(tile("junction_" + i, TileCategory.JUNCTION, 4));
        }
        for (int i = 1; i <= 2; i++) {
            tiles.add(tile("airlock_" + i, TileCategory.AIRLOCK, 3));
        }
        for (int i = 1; i <= 4; i++) {
            tiles.add(tile("aux_" + i, TileCategory.AUXILIARY, 3));
        }

        return List.copyOf(tiles);
    }

    private static TileDefinition tile(
            String id,
            TileCategory category,
            int connectionPointCount
    ) {
        ArrayList<ConnectionPointDefinition> points = new ArrayList<>();

        for (int i = 1; i <= connectionPointCount; i++) {
            points.add(new ConnectionPointDefinition(
                    new ConnectionPointId("p" + i),
                    ConnectionPointType.TELEPORT
            ));
        }

        return new TileDefinition(
                new TileId(id),
                category,
                id,
                points
        );
    }
}
