package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.map.generation.ConstrainedRandomMapGenerator;
import com.hushkisses.spacesurvival.map.generation.GeneratedConnection;
import com.hushkisses.spacesurvival.map.generation.GeneratedMap;
import com.hushkisses.spacesurvival.map.generation.MapGenerationConstraints;
import com.hushkisses.spacesurvival.map.tile.DefaultTileCatalog;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileDefinition;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.config.MatchSetupConfig;
import com.hushkisses.spacesurvival.paper.facility.FacilityTerminalRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PaperShipWorldService {

    public static final String WORLD_NAME = "space_ship_dev";

    private static final int FLOOR_Y = 80;
    private static final int MODULE_SIZE = 15;
    private static final int MODULE_SPACING = 24;
    private static final int COLUMNS = 4;
    private static final String NAV_LABEL_TAG = "space_survival_nav_label";

    private final ConstrainedRandomMapGenerator generator = new ConstrainedRandomMapGenerator();
    private final ShipModuleStructureLoader structureLoader;
    private final FacilityTerminalRegistry terminals;
    private final PhysicalConnectionController connections;

    private PhysicalShipSnapshot activeSnapshot;
    private Map<TileId, StructurePlacementResult> structurePlacements = Map.of();

    public PaperShipWorldService(
            JavaPlugin plugin,
            FacilityTerminalRegistry terminals,
            PhysicalConnectionController connections
    ) {
        this.structureLoader = new ShipModuleStructureLoader(
                Objects.requireNonNull(plugin, "plugin")
        );
        this.terminals = Objects.requireNonNull(terminals, "terminals");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

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
        cleanupNavigationLabels(world);
        clearBuildArea(world);

        terminals.clear();
        connections.reset();

        LinkedHashMap<TileId, PhysicalTilePlacement> placements = new LinkedHashMap<>();
        LinkedHashMap<TileId, StructurePlacementResult> placementResults = new LinkedHashMap<>();

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

            StructurePlacementResult structure = structureLoader.placeIfAvailable(
                    tileId,
                    new Location(world, placement.minX(), FLOOR_Y, placement.minZ()),
                    new Random(seed ^ tileId.value().hashCode())
            );
            placementResults.put(tileId, structure);

            if (!structure.placed()) {
                renderModule(world, placement, definitions.get(tileId));
            }

            renderRoomIdentity(world, placement, definitions.get(tileId));

            FacilityId facilityId = coreFacility(tileId);
            if (facilityId != null) {
                renderFacilityTerminal(world, placement, facilityId);
            }

            index++;
        }

        LinkedHashMap<PortalBlockKey, Location> portals = new LinkedHashMap<>();
        int connectionId = 0;

        for (GeneratedConnection connection : generated.connections()) {
            PhysicalTilePlacement first = placements.get(connection.first().tileId());
            PhysicalTilePlacement second = placements.get(connection.second().tileId());

            Location firstPad = first.portalPad(world, connection.first().pointId());
            Location secondPad = second.portalPad(world, connection.second().pointId());

            renderPortalPad(firstPad);
            renderPortalPad(secondPad);

            portals.put(PortalBlockKey.of(firstPad), second.center(world));
            portals.put(PortalBlockKey.of(secondPad), first.center(world));

            connections.register(
                    connectionId++,
                    connection,
                    firstPad,
                    secondPad
            );

            renderDestinationLabel(
                    firstPad,
                    definitions.get(connection.second().tileId()).displayName()
            );
            renderDestinationLabel(
                    secondPad,
                    definitions.get(connection.first().tileId()).displayName()
            );
        }

        structurePlacements = Collections.unmodifiableMap(placementResults);

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

    public Map<TileId, StructurePlacementResult> structurePlacements() {
        return structurePlacements;
    }

    public ShipModuleStructureLoader structureLoader() {
        return structureLoader;
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
                for (int y = FLOOR_Y; y <= FLOOR_Y + 12; y++) {
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

        Material accent = accent(definition.id(), definition.category());

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

    private void renderFacilityTerminal(
            World world,
            PhysicalTilePlacement placement,
            FacilityId facilityId
    ) {
        Location terminal = new Location(
                world,
                placement.minX() + 2,
                placement.floorY() + 1,
                placement.minZ() + 2
        );

        terminal.getBlock().setType(Material.LODESTONE, false);
        terminals.register(terminal, facilityId);
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

    private static void cleanupNavigationLabels(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity.getScoreboardTags().contains(NAV_LABEL_TAG)) {
                entity.remove();
            }
        }
    }

    private static void renderRoomIdentity(
            World world,
            PhysicalTilePlacement placement,
            TileDefinition definition
    ) {
        Location center = placement.center(world);
        int x = center.getBlockX();
        int z = center.getBlockZ();
        int floor = placement.floorY();
        Material accent = accent(definition.id(), definition.category());

        world.getBlockAt(x, floor, z).setType(accent, false);
        world.getBlockAt(x + 1, floor, z).setType(accent, false);
        world.getBlockAt(x - 1, floor, z).setType(accent, false);
        world.getBlockAt(x, floor, z + 1).setType(accent, false);
        world.getBlockAt(x, floor, z - 1).setType(accent, false);

        Location labelLocation = center.clone().add(0.0, 2.8, 0.0);
        TextDisplay label = world.spawn(labelLocation, TextDisplay.class);
        label.text(Component.text(definition.displayName()));
        label.setBillboard(Display.Billboard.CENTER);
        label.setSeeThrough(true);
        label.setShadowed(true);
        label.setGlowing(true);
        label.addScoreboardTag(NAV_LABEL_TAG);
    }

    private static void renderDestinationLabel(Location pad, String destination) {
        World world = pad.getWorld();
        if (world == null) return;

        Location labelLocation = pad.clone().add(0.0, 2.0, 0.0);
        TextDisplay label = world.spawn(labelLocation, TextDisplay.class);
        label.text(Component.text("→ " + destination));
        label.setBillboard(Display.Billboard.CENTER);
        label.setSeeThrough(true);
        label.setShadowed(true);
        label.addScoreboardTag(NAV_LABEL_TAG);
    }

    private static FacilityId coreFacility(TileId tileId) {
        return switch (tileId.value()) {
            case "bridge", "engineering", "medical", "research", "cargo", "habitation" ->
                    new FacilityId(tileId.value());
            default -> null;
        };
    }

    private static Material accent(TileId tileId, TileCategory category) {
        return switch (tileId.value()) {
            case "bridge" -> Material.BLUE_CONCRETE;
            case "engineering" -> Material.ORANGE_CONCRETE;
            case "medical" -> Material.WHITE_CONCRETE;
            case "research" -> Material.PURPLE_CONCRETE;
            case "cargo" -> Material.YELLOW_CONCRETE;
            case "habitation" -> Material.LIME_CONCRETE;
            default -> switch (category) {
                case CORE -> Material.LIGHT_BLUE_CONCRETE;
                case CORRIDOR -> Material.LIGHT_GRAY_CONCRETE;
                case JUNCTION -> Material.CYAN_CONCRETE;
                case AIRLOCK -> Material.RED_CONCRETE;
                case AUXILIARY -> Material.GRAY_CONCRETE;
            };
        };
    }
}
