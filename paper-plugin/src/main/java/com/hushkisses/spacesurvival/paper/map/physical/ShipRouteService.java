package com.hushkisses.spacesurvival.paper.map.physical;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.map.connection.ConnectionState;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import org.bukkit.entity.Player;

import java.util.*;

public final class ShipRouteService {

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;

    public ShipRouteService(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
    }

    public Optional<RoutePlan> routeToFacility(Player player, FacilityId facilityId) {
        Objects.requireNonNull(facilityId, "facilityId");
        return routeToTile(player, new TileId(facilityId.value()));
    }

    public Optional<RoutePlan> routeToTile(Player player, TileId target) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");

        PhysicalShipSnapshot ship = shipWorldService.activeSnapshot().orElse(null);
        if (ship == null || !ship.placements().containsKey(target)) {
            return Optional.empty();
        }

        TileId current = ship.tileAt(player.getLocation()).orElse(null);
        if (current == null) {
            return Optional.empty();
        }

        List<TileId> usable = shortestPath(ship, player, current, target, true);
        if (!usable.isEmpty()) {
            return Optional.of(new RoutePlan(
                    current,
                    target,
                    usable,
                    nextConnectionState(current, usable),
                    true
            ));
        }

        List<TileId> topology = shortestPath(ship, player, current, target, false);
        if (topology.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RoutePlan(
                current,
                target,
                topology,
                nextConnectionState(current, topology),
                false
        ));
    }

    private List<TileId> shortestPath(
            PhysicalShipSnapshot ship,
            Player player,
            TileId from,
            TileId target,
            boolean traversableOnly
    ) {
        if (from.equals(target)) {
            return List.of(from);
        }

        ArrayDeque<TileId> queue = new ArrayDeque<>();
        Map<TileId, TileId> previous = new HashMap<>();
        Set<TileId> visited = new HashSet<>();

        queue.add(from);
        visited.add(from);

        while (!queue.isEmpty()) {
            TileId current = queue.removeFirst();

            for (TileId next : ship.generatedMap().adjacent(current)) {
                if (visited.contains(next)) {
                    continue;
                }

                ConnectionState state = stateBetween(current, next);
                if (traversableOnly && !canTraverse(player, state)) {
                    continue;
                }

                previous.put(next, current);
                if (next.equals(target)) {
                    return rebuildPath(previous, from, target);
                }

                visited.add(next);
                queue.addLast(next);
            }
        }

        return List.of();
    }

    private List<TileId> rebuildPath(
            Map<TileId, TileId> previous,
            TileId from,
            TileId target
    ) {
        LinkedList<TileId> path = new LinkedList<>();
        TileId cursor = target;
        path.addFirst(cursor);

        while (!cursor.equals(from)) {
            cursor = previous.get(cursor);
            if (cursor == null) {
                return List.of();
            }
            path.addFirst(cursor);
        }

        return List.copyOf(path);
    }

    private ConnectionState nextConnectionState(TileId current, List<TileId> path) {
        if (path.size() < 2) {
            return ConnectionState.OPEN;
        }
        return stateBetween(current, path.get(1));
    }

    private ConnectionState stateBetween(TileId first, TileId second) {
        for (PhysicalConnectionSnapshot snapshot : plugin.physicalConnectionController().snapshots()) {
            var connection = snapshot.connection();
            TileId a = connection.first().tileId();
            TileId b = connection.second().tileId();

            if ((a.equals(first) && b.equals(second))
                    || (a.equals(second) && b.equals(first))) {
                return snapshot.state();
            }
        }
        return ConnectionState.DISABLED;
    }

    private boolean canTraverse(Player player, ConnectionState state) {
        return switch (state) {
            case OPEN -> true;
            case POWER_REQUIRED -> plugin.shipState().power() > 0;
            case KEYCARD_REQUIRED -> plugin.functionalItemService().has(
                    player,
                    FunctionalItemType.SECURITY_KEYCARD
            );
            case LOCKED, DISABLED -> false;
        };
    }

    public record RoutePlan(
            TileId current,
            TileId target,
            List<TileId> path,
            ConnectionState nextConnectionState,
            boolean routeUsable
    ) {
        public RoutePlan {
            path = List.copyOf(path);
        }

        public boolean arrived() {
            return current.equals(target);
        }

        public Optional<TileId> nextTile() {
            return path.size() < 2
                    ? Optional.empty()
                    : Optional.of(path.get(1));
        }

        public boolean nextConnectionUsable() {
            return arrived() || routeUsable;
        }
    }
}
