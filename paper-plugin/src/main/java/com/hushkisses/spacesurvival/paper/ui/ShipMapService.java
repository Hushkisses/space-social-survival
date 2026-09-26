package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.guidance.PlayerGuidanceResolver;
import com.hushkisses.spacesurvival.guidance.PublicProblem;
import com.hushkisses.spacesurvival.map.connection.ConnectionState;
import com.hushkisses.spacesurvival.map.tile.TileCategory;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalConnectionSnapshot;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class ShipMapService implements Listener {

    public static final String TITLE = "§8승무원 PDA — 함선 지도";

    private final SpaceSurvivalPlugin plugin;
    private final PlayerGuidanceResolver guidanceResolver = new PlayerGuidanceResolver();

    public ShipMapService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void open(Player player) {
        PhysicalShipSnapshot ship = plugin.shipWorldService().activeSnapshot().orElse(null);
        if (ship == null) {
            player.sendMessage("§c아직 함선 지도를 사용할 수 없습니다.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, TITLE);
        TileId current = ship.tileAt(player.getLocation()).orElse(null);
        PublicProblem problem = guidanceResolver.resolve(
                plugin.returnObjectiveService().stage(),
                plugin.shipState().snapshot(),
                plugin.facilityRegistry().snapshots()
        );
        TileId target = problem.targetFacility() == null
                ? null
                : new TileId(problem.targetFacility().value());

        inventory.setItem(1, item(
                Material.RECOVERY_COMPASS,
                "§b현재 위치",
                List.of("§f" + (current == null ? "함선 외부/미배치" : ship.tileDisplayName(current)))
        ));

        inventory.setItem(3, item(
                Material.TARGET,
                "§e현재 권장 목적지",
                List.of(
                        "§f" + (target == null ? "없음" : ship.tileDisplayName(target)),
                        "§7" + problem.title()
                )
        ));

        inventory.setItem(5, item(
                Material.COMPASS,
                "§6추천 경로",
                routeLore(ship, current, target)
        ));

        inventory.setItem(7, item(
                Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                "§f통로 상태 범례",
                List.of(
                        "§6금색: §f개방",
                        "§e노랑: §f조건부 통과",
                        "§c빨강: §f차단",
                        "§8지도는 다른 플레이어 위치를 표시하지 않습니다."
                )
        ));

        int slot = 9;
        for (TileId tileId : ship.generatedMap().tileIds()) {
            if (slot >= 54) break;

            var definition = ship.definitions().get(tileId);
            if (definition == null) continue;

            ArrayList<String> lore = new ArrayList<>();
            lore.add("§7구역 유형: §f" + categoryName(definition.category()));
            if (tileId.equals(current)) {
                lore.add("§a현재 위치");
            }
            if (tileId.equals(target)) {
                lore.add("§e현재 권장 목적지");
            }
            lore.add("");
            lore.add("§7연결 구역:");

            List<TileId> adjacent = new ArrayList<>(ship.generatedMap().adjacent(tileId));
            adjacent.sort(Comparator.comparing(TileId::value));
            for (TileId next : adjacent) {
                lore.add(
                        "§7- §f" + ship.tileDisplayName(next)
                                + " " + stateText(connectionState(tileId, next))
                );
            }

            inventory.setItem(
                    slot++,
                    item(
                            materialFor(tileId, definition.category()),
                            (tileId.equals(current) ? "§a▶ " : "§f")
                                    + ship.tileDisplayName(tileId),
                            lore
                    )
            );
        }

        inventory.setItem(49, item(
                Material.ARROW,
                "§aPDA로 돌아가기",
                List.of("§7PDA를 다시 우클릭하면 개인 화면으로 돌아갑니다.")
        ));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (TITLE.equals(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    private List<String> routeLore(
            PhysicalShipSnapshot ship,
            TileId start,
            TileId target
    ) {
        if (start == null) {
            return List.of("§7현재 함선 구역을 확인할 수 없습니다.");
        }
        if (target == null || !ship.generatedMap().tileIds().contains(target)) {
            return List.of("§7현재 지도에 표시할 목적지가 없습니다.");
        }
        if (start.equals(target)) {
            return List.of("§a이미 목적지에 도착했습니다.");
        }

        List<TileId> route = shortestPath(ship, start, target);
        if (route.isEmpty()) {
            return List.of("§c현재 목적지로 가는 경로를 찾을 수 없습니다.");
        }

        ArrayList<String> lore = new ArrayList<>();
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < route.size(); i++) {
            if (i > 0) path.append(" §8→ §f");
            path.append(ship.tileDisplayName(route.get(i)));
        }
        lore.add("§f" + path);
        lore.add("§8통로가 빨간색이면 다른 경로 또는 복구가 필요할 수 있습니다.");
        return lore;
    }

    private List<TileId> shortestPath(
            PhysicalShipSnapshot ship,
            TileId start,
            TileId target
    ) {
        ArrayDeque<TileId> queue = new ArrayDeque<>();
        Map<TileId, TileId> previous = new HashMap<>();

        queue.add(start);
        previous.put(start, null);

        while (!queue.isEmpty()) {
            TileId current = queue.removeFirst();
            if (current.equals(target)) break;

            for (TileId next : ship.generatedMap().adjacent(current)) {
                if (!previous.containsKey(next)) {
                    previous.put(next, current);
                    queue.addLast(next);
                }
            }
        }

        if (!previous.containsKey(target)) {
            return List.of();
        }

        ArrayList<TileId> route = new ArrayList<>();
        for (TileId at = target; at != null; at = previous.get(at)) {
            route.add(at);
        }
        Collections.reverse(route);
        return List.copyOf(route);
    }

    private ConnectionState connectionState(TileId first, TileId second) {
        return plugin.physicalConnectionController().snapshots().stream()
                .filter(snapshot -> connects(snapshot, first, second))
                .map(PhysicalConnectionSnapshot::state)
                .findFirst()
                .orElse(ConnectionState.OPEN);
    }

    private static boolean connects(
            PhysicalConnectionSnapshot snapshot,
            TileId first,
            TileId second
    ) {
        TileId a = snapshot.connection().first().tileId();
        TileId b = snapshot.connection().second().tileId();
        return (a.equals(first) && b.equals(second))
                || (a.equals(second) && b.equals(first));
    }

    private static String stateText(ConnectionState state) {
        return switch (state) {
            case OPEN -> "§8[§6개방§8]";
            case POWER_REQUIRED -> "§8[§e전력 필요§8]";
            case KEYCARD_REQUIRED -> "§8[§e키카드 필요§8]";
            case LOCKED -> "§8[§c잠김§8]";
            case DISABLED -> "§8[§c사용 불가§8]";
        };
    }

    private static Material materialFor(TileId tileId, TileCategory category) {
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

    private static String categoryName(TileCategory category) {
        return switch (category) {
            case CORE -> "핵심 시설";
            case CORRIDOR -> "연결 복도";
            case JUNCTION -> "교차 구역";
            case AIRLOCK -> "에어록";
            case AUXILIARY -> "보조 구역";
        };
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
