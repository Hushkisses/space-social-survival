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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Display;
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

    private final ConstrainedRandomMapGenerator generator = new ConstrainedRandomMapGenerator();
    private final ShipModuleStructureLoader structureLoader;
    private final FacilityTerminalRegistry terminals;
    private final PhysicalConnectionController connections;

    private PhysicalShipSnapshot activeSnapshot;
    private Map<TileId, StructurePlacementResult> structurePlacements = Map.of();
    private final Map<RouteDirection, TextDisplay> portalLabels = new LinkedHashMap<>();
    private TileId highlightedRouteTarget;

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
        clearNavigationDisplays(world);
        clearBuildArea(world);

        terminals.clear();
        connections.reset();
        portalLabels.clear();
        highlightedRouteTarget = null;

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

            renderRoomLabel(
                    world,
                    placement,
                    definitions.get(tileId)
            );

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
            TextDisplay firstLabel = renderPortalDestinationLabel(
                    firstPad,
                    definitions.get(connection.second().tileId())
            );
            TextDisplay secondLabel = renderPortalDestinationLabel(
                    secondPad,
                    definitions.get(connection.first().tileId())
            );
            if (firstLabel != null) {
                portalLabels.put(
                        new RouteDirection(
                                connection.first().tileId(),
                                connection.second().tileId()
                        ),
                        firstLabel
                );
            }
            if (secondLabel != null) {
                portalLabels.put(
                        new RouteDirection(
                                connection.second().tileId(),
                                connection.first().tileId()
                        ),
                        secondLabel
                );
            }

            portals.put(PortalBlockKey.of(firstPad), second.center(world));
            portals.put(PortalBlockKey.of(secondPad), first.center(world));

            connections.register(
                    connectionId++,
                    connection,
                    firstPad,
                    secondPad
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

    public void updatePriorityRoute(FacilityId facilityId) {
        Objects.requireNonNull(facilityId, "facilityId");

        PhysicalShipSnapshot ship = activeSnapshot;
        if (ship == null) {
            return;
        }

        TileId target = new TileId(facilityId.value());
        if (!ship.generatedMap().tileIds().contains(target)) {
            clearPriorityRoute();
            return;
        }

        if (target.equals(highlightedRouteTarget)) {
            return;
        }

        highlightedRouteTarget = target;

        for (Map.Entry<RouteDirection, TextDisplay> entry : portalLabels.entrySet()) {
            RouteDirection direction = entry.getKey();
            TextDisplay display = entry.getValue();

            if (!display.isValid()) {
                continue;
            }

            int fromDistance = ship.generatedMap().distance(direction.from(), target);
            int toDistance = ship.generatedMap().distance(direction.to(), target);
            boolean towardTarget = !direction.from().equals(target)
                    && toDistance < fromDistance;

            renderPortalDestinationText(
                    display,
                    ship.definitions().get(direction.to()),
                    towardTarget
            );
        }
    }

    public void clearPriorityRoute() {
        PhysicalShipSnapshot ship = activeSnapshot;
        highlightedRouteTarget = null;

        if (ship == null) {
            return;
        }

        for (Map.Entry<RouteDirection, TextDisplay> entry : portalLabels.entrySet()) {
            TextDisplay display = entry.getValue();
            if (!display.isValid()) {
                continue;
            }
            renderPortalDestinationText(
                    display,
                    ship.definitions().get(entry.getKey().to()),
                    false
            );
        }
    }

    private static void clearNavigationDisplays(World world) {
        world.getEntitiesByClass(TextDisplay.class).stream()
                .filter(entity -> entity.getScoreboardTags().contains("spacesurvival_nav"))
                .forEach(TextDisplay::remove);
    }

    private static void renderRoomLabel(
            World world,
            PhysicalTilePlacement placement,
            TileDefinition definition
    ) {
        if (definition == null) return;

        Location location = placement.center(world).add(0.0, 3.1, 0.0);
        TextDisplay display = world.spawn(location, TextDisplay.class);
        display.addScoreboardTag("spacesurvival_nav");
        display.setBillboard(Display.Billboard.CENTER);
        display.text(
                Component.text("◆ ", accentColor(definition.category()))
                        .append(Component.text(definition.displayName(), NamedTextColor.WHITE))
        );
    }

    private static TextDisplay renderPortalDestinationLabel(
            Location pad,
            TileDefinition destination
    ) {
        if (pad.getWorld() == null || destination == null) return null;

        Location labelLocation = pad.clone().add(0.0, 1.65, 0.0);
        TextDisplay display = pad.getWorld().spawn(labelLocation, TextDisplay.class);
        display.addScoreboardTag("spacesurvival_nav");
        display.addScoreboardTag("spacesurvival_portal_label");
        display.setBillboard(Display.Billboard.CENTER);
        renderPortalDestinationText(display, destination, false);
        return display;
    }

    private static void renderPortalDestinationText(
            TextDisplay display,
            TileDefinition destination,
            boolean priority
    ) {
        if (destination == null) return;

        if (priority) {
            display.text(
                    Component.text("◆ ", NamedTextColor.RED)
                            .append(Component.text(destination.displayName(), NamedTextColor.RED))
            );
            return;
        }

        display.text(
                Component.text("→ ", NamedTextColor.YELLOW)
                        .append(Component.text(destination.displayName(), NamedTextColor.WHITE))
        );
    }

    private static NamedTextColor accentColor(TileCategory category) {
        return switch (category) {
            case CORE -> NamedTextColor.AQUA;
            case CORRIDOR -> NamedTextColor.WHITE;
            case JUNCTION -> NamedTextColor.YELLOW;
            case AIRLOCK -> NamedTextColor.GOLD;
            case AUXILIARY -> NamedTextColor.GREEN;
        };
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

    private static FacilityId coreFacility(TileId tileId) {
        return switch (tileId.value()) {
            case "bridge", "engineering", "medical", "research", "cargo", "habitation" ->
                    new FacilityId(tileId.value());
            default -> null;
        };
    }

    private record RouteDirection(TileId from, TileId to) {
        private RouteDirection {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
        }
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
