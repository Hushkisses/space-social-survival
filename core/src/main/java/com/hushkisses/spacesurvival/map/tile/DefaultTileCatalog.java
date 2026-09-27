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
        tiles.add(tile("airlock_1", TileCategory.AIRLOCK, "주 에어록", 3));
        tiles.add(tile("airlock_2", TileCategory.AIRLOCK, "보조 에어록", 3));

        tiles.add(tile("auxiliary_1", TileCategory.AUXILIARY, "정비실", 3));
        tiles.add(tile("auxiliary_2", TileCategory.AUXILIARY, "비상 창고", 3));
        tiles.add(tile("auxiliary_3", TileCategory.AUXILIARY, "통신 보조실", 3));
        tiles.add(tile("auxiliary_4", TileCategory.AUXILIARY, "격리실", 3));

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
