package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.map.generation.ConstrainedRandomMapGenerator;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;
import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.map.generation.MapGenerationConstraints;
import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.config.MatchSetupConfig;
import org.bukkit.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperShipWorldService {

    public static final String WORLD_NAME = "space_ship_dev";

    private static final int FLOOR_Y = 80;
    private static final int MODULE_SIZE = 15;
    private static final int MODULE_SPACING = 24;
    private static final int COLUMNS = 4;

    private final ConstrainedRandomMapGenerator generator = new ConstrainedRandomMapGenerator();
    private PhysicalShipSnapshot activeSnapshot;

    public PhysicalShipSnapshot generateAndRender(long seed, MatchSetupConfig config) {
        Objects.requireNonNull(config, "config");

        List<TileDefinition> pool = DefaultTileCatalog.create();
        Map<TileId, TileDefinition> definitions = pool.stream()
                .collect(Collectors.toUnmodifiableMap(TileDefinition::id, Function.identity()));

        GeneratedMap generated = generator.generate(
                pool,
                new MapGenerationConstraints(
                        config.mapMinTiles(),
                        config.mapMaxTiles(),
                        config.mapMaxDeadEnds(),
                        config.mapMaxCoreDistance(),
                        config.mapMaxAttempts()
                ),
                new Random(seed)
        );

        World world = resolveWorld();
        clearBuildArea(world);

        LinkedHashMap<TileId, PhysicalTilePlacement> placements = new LinkedHashMap<>();
        int index = 0;
        for (TileId tileId : generated.tileIds()) {
            int col = index % COLUMNS;
            int row = index / COLUMNS;
            PhysicalTilePlacement placement = new PhysicalTilePlacement(
                    tileId,
                    col * MODULE_SPACING,
                    FLOOR_Y,
                    row * MODULE_SPACING,
                    MODULE_SIZE
            );
            placements.put(tileId, placement);
            renderModule(world, placement, definitions.get(tileId));
            index++;
        }

        LinkedHashMap<PortalBlockKey, Location> portals = new LinkedHashMap<>();
        for (GeneratedConnection connection : generated.connections()) {
            PhysicalTilePlacement first = placements.get(connection.first().tileId());
            PhysicalTilePlacement second = placements.get(connection.second().tileId());

            Location firstPad = first.portalPad(world, connection.first().pointId());
            Location secondPad = second.portalPad(world, connection.second().pointId());

            renderPortalPad(firstPad);
            renderPortalPad(secondPad);

            portals.put(PortalBlockKey.of(firstPad), second.center(world));
            portals.put(PortalBlockKey.of(secondPad), first.center(world));
        }

        activeSnapshot = new PhysicalShipSnapshot(
                seed,
                world,
                generated,
                definitions,
                placements,
                portals
        );
        return activeSnapshot;
    }

    public Optional<PhysicalShipSnapshot> activeSnapshot() {
        return Optional.ofNullable(activeSnapshot);
    }

    private static World resolveWorld() {
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing != null) {
            return existing;
        }

        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);

        World world = creator.createWorld();
        if (world == null) {
            throw new IllegalStateException("Could not create world: " + WORLD_NAME);
        }

        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(6000L);
        return world;
    }

    private static void clearBuildArea(World world) {
        int maxX = COLUMNS * MODULE_SPACING;
        int maxZ = COLUMNS * MODULE_SPACING;

        for (int x = -2; x <= maxX; x++) {
            for (int z = -2; z <= maxZ; z++) {
                for (int y = FLOOR_Y; y <= FLOOR_Y + 7; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }
            }
        }
    }

    private static void renderModule(
            World world,
            PhysicalTilePlacement placement,
            TileDefinition definition
    ) {
        int minX = placement.minX();
        int minZ = placement.minZ();
        int maxX = minX + placement.size() - 1;
        int maxZ = minZ + placement.size() - 1;
        int floor = placement.floorY();

        Material accent = accent(definition.category());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(x, floor, z).setType(Material.SMOOTH_STONE, false);

                boolean wall = x == minX || x == maxX || z == minZ || z == maxZ;
                if (wall) {
                    for (int y = floor + 1; y <= floor + 4; y++) {
                        world.getBlockAt(x, y, z).setType(Material.GRAY_CONCRETE, false);
                    }
                }
            }
        }

        Location center = placement.center(world);
        world.getBlockAt(center.getBlockX(), floor, center.getBlockZ())
                .setType(accent, false);

        world.getBlockAt(minX + 1, floor + 1, minZ + 1)
                .setType(Material.SEA_LANTERN, false);
        world.getBlockAt(maxX - 1, floor + 1, maxZ - 1)
                .setType(Material.SEA_LANTERN, false);
    }

    private static void renderPortalPad(Location pad) {
        World world = pad.getWorld();
        if (world == null) return;

        int x = pad.getBlockX();
        int y = pad.getBlockY();
        int z = pad.getBlockZ();

        world.getBlockAt(x, y - 1, z).setType(Material.GOLD_BLOCK, false);
        world.getBlockAt(x, y, z).setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, false);
    }

    private static Material accent(TileCategory category) {
        return switch (category) {
            case CORE -> Material.LIGHT_BLUE_CONCRETE;
            case CORRIDOR -> Material.WHITE_CONCRETE;
            case JUNCTION -> Material.YELLOW_CONCRETE;
            case AIRLOCK -> Material.ORANGE_CONCRETE;
            case AUXILIARY -> Material.LIME_CONCRETE;
        };
    }
}
