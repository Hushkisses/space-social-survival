package com.hushkisses.spacesurvival.map.tile;

import java.util.ArrayList;
import java.util.List;

public final class DefaultTileCatalog {

    private DefaultTileCatalog() {
    }

    public static List<TileDefinition> create() {
        ArrayList<TileDefinition> tiles = new ArrayList<>();

        tiles.add(tile("bridge", TileCategory.CORE, "함교", 4));
        tiles.add(tile("engineering", TileCategory.CORE, "기관실", 4));
        tiles.add(tile("medical", TileCategory.CORE, "의료실", 4));
        tiles.add(tile("research", TileCategory.CORE, "연구실", 4));
        tiles.add(tile("cargo", TileCategory.CORE, "화물실", 4));
        tiles.add(tile("habitation", TileCategory.CORE, "생활구역", 4));

        for (int i = 1; i <= 5; i++) {
            tiles.add(tile("corridor_" + i, TileCategory.CORRIDOR, "연결 복도 " + i, 3));
        }
        for (int i = 1; i <= 3; i++) {
            tiles.add(tile("junction_" + i, TileCategory.JUNCTION, "교차 구역 " + i, 4));
        }
        for (int i = 1; i <= 2; i++) {
            tiles.add(tile("airlock_" + i, TileCategory.AIRLOCK, "에어록 " + i, 3));
        }
        for (int i = 1; i <= 4; i++) {
            tiles.add(tile("auxiliary_" + i, TileCategory.AUXILIARY, "보조 구역 " + i, 3));
        }

        return List.copyOf(tiles);
    }

    public static TileRegistry createRegistry() {
        TileRegistry registry = new TileRegistry();
        create().forEach(registry::register);
        return registry;
    }

    private static TileDefinition tile(
            String id,
            TileCategory category,
            String displayName,
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
                displayName,
                points
        );
    }
}
