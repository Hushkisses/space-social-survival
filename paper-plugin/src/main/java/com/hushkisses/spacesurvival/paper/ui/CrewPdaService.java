package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.ending.ReturnRequirements;
import com.hushkisses.spacesurvival.guidance.PlayerGuidanceResolver;
import com.hushkisses.spacesurvival.guidance.PublicProblem;
import com.hushkisses.spacesurvival.infection.InfectionStage;
import com.hushkisses.spacesurvival.objective.ObjectiveInstance;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.objective.ObjectiveStatus;
import com.hushkisses.spacesurvival.map.tile.TileId;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import com.hushkisses.spacesurvival.paper.map.physical.PaperShipWorldService;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.role.RoleCapability;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.social.sanction.PlayerSanctionState;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class CrewPdaService implements Listener {

    public static final String PERSONAL_TITLE = "§8승무원 PDA — 개인 정보";
    public static final String PUBLIC_TITLE = "§8승무원 PDA — 공용 상태";
    public static final String HELP_TITLE = "§8승무원 PDA — 초보 도움말";
    public static final String MAP_TITLE = "§8승무원 PDA — 함선 지도";

    private final SpaceSurvivalPlugin plugin;
    private final PaperShipWorldService shipWorldService;
    private final PlayerGuidanceResolver guidanceResolver = new PlayerGuidanceResolver();
    private final NamespacedKey pdaKey;
    private final NamespacedKey actionKey;

    public CrewPdaService(
            SpaceSurvivalPlugin plugin,
            PaperShipWorldService shipWorldService
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.shipWorldService = Objects.requireNonNull(shipWorldService, "shipWorldService");
        this.pdaKey = new NamespacedKey(plugin, "crew_pda");
        this.actionKey = new NamespacedKey(plugin, "crew_pda_action");
    }

    public void giveToParticipants() {
        for (PlayerId playerId : plugin.lobbyService().snapshot().players()) {
            Player player = plugin.getServer().getPlayer(playerId.value());
            if (player == null) continue;
            ensurePda(player);
            player.sendMessage("§b[PDA] §f승무원 PDA가 지급되었습니다. §e우클릭§f하면 언제든 역할·목표·공용 상태를 확인할 수 있습니다.");
            PublicProblem problem = currentProblem();
            String target = plugin.facilityRegistry().require(problem.targetFacility())
                    .definition().displayName();
            player.sendMessage("§6[첫 행동] §f" + problem.title() + " → §e" + target);
            player.sendMessage("§7" + problem.nextAction());
        }
    }

    public void ensurePda(Player player) {
        if (hasPda(player)) return;

        ItemStack pda = createPda();
        ItemStack slotEight = player.getInventory().getItem(8);
        if (slotEight == null || slotEight.getType().isAir()) {
            player.getInventory().setItem(8, pda);
            return;
        }

        var overflow = player.getInventory().addItem(pda);
        if (!overflow.isEmpty()) {
            ItemStack displaced = slotEight.clone();
            player.getInventory().setItem(8, pda);

            var displacedOverflow = player.getInventory().addItem(displaced);
            displacedOverflow.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item)
            );
        }
    }

    public void open(Player player) {
        PlayerId playerId = PlayerId.of(player.getUniqueId());
        if (!plugin.lobbyService().contains(playerId)) {
            player.sendMessage("§c현재 우주 생존 게임 참가자가 아닙니다.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 45, PERSONAL_TITLE);
        RoleId roleId = plugin.roleSelectionService().selectedRole(playerId).orElse(null);
        RoleDefinition role = roleId == null ? null : plugin.roleRegistry().require(roleId);

        inventory.setItem(4, situationItem());
        inventory.setItem(10, roleItem(role));
        inventory.setItem(12, equipmentItem(player, roleId));
        inventory.setItem(14, objectiveItem(
                "§e기본 개인 목표",
                plugin.objectiveEngine().objective(playerId, ObjectiveSlot.BASE).orElse(null),
                Material.MAP
        ));
        inventory.setItem(16, objectiveItem(
                "§d비밀 임무",
                plugin.objectiveEngine().objective(playerId, ObjectiveSlot.SECRET).orElse(null),
                Material.ENDER_EYE
        ));
        inventory.setItem(28, playerStatusItem(playerId));
        inventory.setItem(30, locationItem(player));
        inventory.setItem(32, nextActionItem());
        inventory.setItem(34, actionItem(
                Material.COMPASS,
                "§b공용 상태 상세",
                "public",
                List.of("§7함선 수치·귀환 단계·시설 상태를 확인합니다.", "§a클릭")
        ));
        inventory.setItem(38, actionItem(
                Material.FILLED_MAP,
                "§b함선 지도",
                "map",
                List.of(
                        "§7발견된 함선 모듈과 연결 상태를 확인합니다.",
                        "§7실시간 플레이어 위치는 표시하지 않습니다.",
                        "§a클릭"
                )
        ));
        inventory.setItem(40, actionItem(
                Material.KNOWLEDGE_BOOK,
                "§e초보 도움말",
                "help",
                List.of("§7이동·시설·자원·회의·사망 규칙을 다시 확인합니다.", "§a클릭")
        ));

        player.openInventory(inventory);
    }

    private void openPublic(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, PUBLIC_TITLE);
        var ship = plugin.shipState().snapshot();
        PublicProblem problem = currentProblem();

        inventory.setItem(4, item(
                Material.REDSTONE_TORCH,
                "§c현재 최우선 공개 문제",
                List.of(
                        "§f" + problem.title(),
                        "§7대상 시설: §f" + plugin.facilityRegistry()
                                .require(problem.targetFacility()).definition().displayName(),
                        "§7필요: §f" + problem.need(),
                        "§7행동: §f" + problem.nextAction()
                )
        ));
        inventory.setItem(10, item(
                Material.REDSTONE,
                "§f함선 핵심 수치",
                List.of(
                        "§7전력: §f" + ship.power() + "%",
                        "§7산소: §f" + ship.oxygen() + "%",
                        "§7선체: §f" + ship.hull() + "%",
                        "§7원자로: §f" + ship.reactor() + "%"
                )
        ));
        inventory.setItem(12, item(
                Material.COMPASS,
                "§b귀환 단계",
                List.of("§f" + MatchHudService.stageName(plugin.returnObjectiveService().stage()))
        ));
        inventory.setItem(14, item(
                Material.CLOCK,
                "§6위기 단계",
                List.of("§f" + MatchHudService.crisisName(plugin.gameRuntimeService().currentCrisisStage()))
        ));
        inventory.setItem(16, item(
                Material.CHEST,
                "§e공용 핵심 자원",
                sharedResourceLore()
        ));

        int slot = 27;
        for (var facility : plugin.facilityRegistry().snapshots()) {
            inventory.setItem(slot++, item(
                    facilityMaterial(facility.status()),
                    "§f" + facility.displayName(),
                    List.of("§7상태: §f" + facilityStatusName(facility.status()))
            ));
        }

        inventory.setItem(40, actionItem(
                Material.ARROW,
                "§a개인 정보로 돌아가기",
                "personal",
                List.of("§a클릭")
        ));
        player.openInventory(inventory);
    }

    private void openHelp(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, HELP_TITLE);

        inventory.setItem(10, item(
                Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
                "§e함선 이동",
                List.of(
                        "§7핵심 시설은 중앙 허브 주변에 촘촘히 배치되어 있습니다.",
                        "§7바닥 색 노선과 중앙 허브 표지판을 따라 이동하십시오.",
                        "§7PDA 함선 지도는 실제 방의 상대 위치와 같은 평면도입니다.",
                        "§7막힌 문은 상태나 권한 조건을 확인하십시오."
                )
        ));
        inventory.setItem(12, item(
                Material.LECTERN,
                "§b시설 콘솔",
                List.of(
                        "§7각 핵심 구역의 콘솔을 우클릭하면 작업 UI가 열립니다.",
                        "§7기본 기능은 누구나, 고급 기능은 직업·장비 조건이 필요할 수 있습니다."
                )
        ));
        inventory.setItem(14, item(
                Material.BARREL,
                "§6자원",
                List.of(
                        "§7비상 보급 상자에서 물리 자원을 확보합니다.",
                        "§7소지 자원은 화물실에서 공용 창고에 입고해야",
                        "§7대부분의 시설 작업에 사용할 수 있습니다."
                )
        ));
        inventory.setItem(16, item(
                Material.BELL,
                "§d회의",
                List.of(
                        "§7함교에서 회의를 소집할 수 있습니다.",
                        "§7투표 결과만으로 모든 처분이 마법처럼 실행되지는 않으며",
                        "§7직업·시설·권한 조건이 필요할 수 있습니다."
                )
        ));
        inventory.setItem(28, item(
                Material.SKELETON_SKULL,
                "§7사망과 통신",
                List.of(
                        "§7사망자는 생존자와 일반적으로 직접 대화할 수 없습니다.",
                        "§7개인 목표는 게임 종료 전까지 공개되지 않습니다.",
                        "§7감염 시나리오에서는 사망 후 형태가 달라질 수 있습니다."
                )
        ));
        inventory.setItem(30, item(
                Material.IRON_PICKAXE,
                "§f복구와 직업 장비",
                List.of(
                        "§7전력·원자로: 기관실",
                        "§7산소·생명유지: 생활구역",
                        "§7선체 균열: 화물실에서 수리 부품 수령 후 현장 우클릭",
                        "§7고급 행동은 직업 권한·장비가 필요할 수 있습니다."
                )
        ));
        inventory.setItem(32, item(
                Material.SPYGLASS,
                "§5정보 원칙",
                List.of(
                        "§7시스템이 보여주는 정보는 해당 범위에서는 사실입니다.",
                        "§7단, 숨은 원인·다른 플레이어의 개인 목표·",
                        "§7탐지되지 않은 감염 정보는 공개되지 않습니다."
                )
        ));
        inventory.setItem(40, actionItem(
                Material.ARROW,
                "§a개인 정보로 돌아가기",
                "personal",
                List.of("§a클릭")
        ));
        player.openInventory(inventory);
    }

    private void openMap(Player player) {
        var ship = shipWorldService.activeSnapshot().orElse(null);
        if (ship == null) {
            player.sendMessage("§c아직 생성된 함선 지도가 없습니다.");
            return;
        }

        var mapImage = plugin.itemsAdderBridge()
                .fontImage("spacesurvival:ship_map_gui", -8);
        boolean imageMode = mapImage.isPresent()
                && plugin.itemsAdderBridge()
                .createItem("spacesurvival:map_hotspot")
                .isPresent();

        MapInventoryHolder holder = new MapInventoryHolder();
        String mapTitle = imageMode
                ? "§f" + mapImage.orElseThrow()
                : MAP_TITLE;
        Inventory inventory = Bukkit.createInventory(
                holder,
                imageMode ? 45 : 54,
                mapTitle
        );
        holder.bind(inventory);

        TileId current = ship.tileAt(player.getLocation()).orElse(null);
        TileId target = mapTargetTile();

        if (imageMode) {
            renderImageMap(inventory, ship, current, target, player);
        } else {
            renderVanillaMap(inventory, ship, current, target, player);
        }

        player.openInventory(inventory);
    }

    private void renderImageMap(
            Inventory inventory,
            com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot ship,
            TileId current,
            TileId target,
            Player player
    ) {
        Map<TileId, Integer> slots = mapSlots();

        for (Map.Entry<TileId, Integer> entry : slots.entrySet()) {
            TileId tileId = entry.getKey();
            if (!ship.placements().containsKey(tileId)) {
                continue;
            }

            boolean isCurrent = tileId.equals(current);
            boolean isTarget = tileId.equals(target);

            String markerId;
            if (isCurrent && isTarget) {
                markerId = "spacesurvival:map_current_target_marker";
            } else if (isCurrent) {
                markerId = "spacesurvival:map_current_marker";
            } else if (isTarget) {
                markerId = "spacesurvival:map_target_marker";
            } else if (!isCoreMapRoom(tileId)
                    && !tileId.value().equals("junction_1")) {
                markerId = "spacesurvival:map_optional_marker";
            } else {
                markerId = "spacesurvival:map_hotspot";
            }

            inventory.setItem(
                    entry.getValue(),
                    imageMapMarker(ship, tileId, markerId, isCurrent, isTarget)
            );
        }

        String currentName = current == null
                ? "함선 주 통로"
                : ship.tileDisplayName(current);
        String targetName = ship.placements().containsKey(target)
                ? ship.tileDisplayName(target)
                : "현재 목표";

        inventory.setItem(
                36,
                themedMapItem(
                        "spacesurvival:map_info_button",
                        item(
                                Material.RECOVERY_COMPASS,
                                "§f현재/목표",
                                List.of(
                                        "§b● 현재: §f" + currentName,
                                        "§e◆ 목표: §f" + targetName,
                                        "",
                                        "§7지도 색상은 실제 바닥 노선과 같습니다."
                                )
                        )
                )
        );
        inventory.setItem(
                38,
                themedMapItem(
                        "spacesurvival:map_route_button",
                        routeSummaryItem(player, ship)
                )
        );
        inventory.setItem(
                40,
                themedMapActionItem(
                        "spacesurvival:map_back_button",
                        Material.ARROW,
                        "§a개인 정보로 돌아가기",
                        "personal",
                        List.of("§a클릭")
                )
        );
    }

    private void renderVanillaMap(
            Inventory inventory,
            com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot ship,
            TileId current,
            TileId target,
            Player player
    ) {
        inventory.setItem(4, item(
                Material.FILLED_MAP,
                "§b함선 평면도",
                List.of(
                        "§7실제 함선의 상대 위치와 같은 배치입니다.",
                        "§a● §7현재 위치  §e◆ §7긴급 목표",
                        "",
                        "§b청록 §7함교",
                        "§c빨강 §7기관실",
                        "§a초록 §7생활구역",
                        "§6주황 §7화물실",
                        "§d분홍 §7의료실",
                        "§5보라 §7연구실"
                )
        ));

        putMapRoom(inventory, ship, 9, new TileId("auxiliary_1"), current, target);
        putMapRoom(inventory, ship, 13, new TileId("habitation"), current, target);
        putMapRoom(inventory, ship, 16, new TileId("cargo"), current, target);
        putMapRoom(inventory, ship, 17, new TileId("auxiliary_3"), current, target);

        putMapRoom(inventory, ship, 19, new TileId("bridge"), current, target);
        putMapRoom(inventory, ship, 22, new TileId("junction_1"), current, target);
        putMapRoom(inventory, ship, 25, new TileId("engineering"), current, target);
        putMapRoom(inventory, ship, 26, new TileId("airlock_1"), current, target);

        putMapRoom(inventory, ship, 27, new TileId("auxiliary_2"), current, target);
        putMapRoom(inventory, ship, 31, new TileId("medical"), current, target);
        putMapRoom(inventory, ship, 34, new TileId("research"), current, target);
        putMapRoom(inventory, ship, 35, new TileId("auxiliary_4"), current, target);

        inventory.setItem(20, mapLine(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b함교 노선"));
        inventory.setItem(21, mapLine(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "§b함교 노선"));
        inventory.setItem(23, mapLine(Material.RED_STAINED_GLASS_PANE, "§c기관실 노선"));
        inventory.setItem(24, mapLine(Material.RED_STAINED_GLASS_PANE, "§c기관실 노선"));
        inventory.setItem(14, mapLine(Material.ORANGE_STAINED_GLASS_PANE, "§6화물 노선"));
        inventory.setItem(15, mapLine(Material.ORANGE_STAINED_GLASS_PANE, "§6화물 노선"));
        inventory.setItem(32, mapLine(Material.PURPLE_STAINED_GLASS_PANE, "§5연구 노선"));
        inventory.setItem(33, mapLine(Material.PURPLE_STAINED_GLASS_PANE, "§5연구 노선"));

        String currentName = current == null
                ? "함선 주 통로"
                : ship.tileDisplayName(current);
        String targetName = ship.placements().containsKey(target)
                ? ship.tileDisplayName(target)
                : "현재 목표";

        inventory.setItem(46, item(
                Material.RECOVERY_COMPASS,
                "§f현재/목표",
                List.of(
                        "§a● 현재: §f" + currentName,
                        "§e◆ 목표: §f" + targetName,
                        "",
                        "§7중앙 허브와 바닥 색 노선을 기준으로 이동하십시오."
                )
        ));
        inventory.setItem(47, routeSummaryItem(player, ship));
        inventory.setItem(49, actionItem(
                Material.ARROW,
                "§a개인 정보로 돌아가기",
                "personal",
                List.of("§a클릭")
        ));
    }

    private Map<TileId, Integer> mapSlots() {
        LinkedHashMap<TileId, Integer> slots = new LinkedHashMap<>();
        slots.put(new TileId("auxiliary_1"), 9);
        slots.put(new TileId("habitation"), 13);
        slots.put(new TileId("cargo"), 16);
        slots.put(new TileId("auxiliary_3"), 17);
        slots.put(new TileId("bridge"), 19);
        slots.put(new TileId("junction_1"), 22);
        slots.put(new TileId("engineering"), 25);
        slots.put(new TileId("airlock_1"), 26);
        slots.put(new TileId("auxiliary_2"), 27);
        slots.put(new TileId("medical"), 31);
        slots.put(new TileId("research"), 34);
        slots.put(new TileId("auxiliary_4"), 35);
        return slots;
    }

    private ItemStack imageMapMarker(
            com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot ship,
            TileId tileId,
            String markerId,
            boolean isCurrent,
            boolean isTarget
    ) {
        var definition = ship.definitions().get(tileId);
        if (definition == null) {
            return themedMapItem(
                    markerId,
                    item(Material.PAPER, "§f" + tileId.value(), List.of())
            );
        }

        ArrayList<String> lore = new ArrayList<>();
        if (isCurrent) {
            lore.add("§b● 현재 위치");
        }
        if (isTarget) {
            lore.add("§e◆ 현재 긴급 목표");
        }
        lore.add("§7분류: §f" + tileCategoryName(definition.category()));
        if (isCoreMapRoom(tileId)) {
            lore.add("§7바닥 노선: " + mapRouteName(tileId));
        }
        lore.add("");
        lore.add("§7인접 구역:");
        for (TileId adjacent : ship.generatedMap().adjacent(tileId)) {
            lore.add(
                    connectionColor(connectionState(tileId, adjacent))
                            + "- "
                            + ship.tileDisplayName(adjacent)
                            + " §8["
                            + connectionStateName(connectionState(tileId, adjacent))
                            + "]"
            );
        }

        return themedMapItem(
                markerId,
                item(
                        Material.PAPER,
                        (isCurrent ? "§b● " : "")
                                + (isTarget ? "§e◆ " : "")
                                + "§f"
                                + definition.displayName(),
                        lore
                )
        );
    }

    private ItemStack themedMapItem(String namespacedId, ItemStack fallback) {
        ItemStack result = plugin.itemsAdderBridge()
                .createItem(namespacedId)
                .orElseGet(fallback::clone);

        ItemMeta source = fallback.getItemMeta();
        ItemMeta target = result.getItemMeta();
        target.setDisplayName(source.getDisplayName());
        target.setLore(source.getLore());
        result.setItemMeta(target);
        return result;
    }

    private ItemStack themedMapActionItem(
            String namespacedId,
            Material fallbackMaterial,
            String name,
            String action,
            List<String> lore
    ) {
        ItemStack result = plugin.itemsAdderBridge()
                .createItem(namespacedId)
                .orElseGet(() -> new ItemStack(fallbackMaterial));

        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(
                actionKey,
                PersistentDataType.STRING,
                action
        );
        result.setItemMeta(meta);
        return result;
    }

    private void putMapRoom(
            Inventory inventory,
            com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot ship,
            int slot,
            TileId tileId,
            TileId current,
            TileId target
    ) {
        if (!ship.placements().containsKey(tileId)) {
            return;
        }

        var definition = ship.definitions().get(tileId);
        if (definition == null) {
            return;
        }

        boolean isCurrent = tileId.equals(current);
        boolean isTarget = tileId.equals(target);

        String prefix = "";
        if (isCurrent) prefix += "§a● ";
        if (isTarget) prefix += "§e◆ ";
        if (prefix.isEmpty()) prefix = "§f";

        ArrayList<String> lore = new ArrayList<>();
        if (isCurrent) {
            lore.add("§a현재 위치");
        }
        if (isTarget) {
            lore.add("§e현재 긴급 목표");
        }

        lore.add("§7분류: §f" + tileCategoryName(definition.category()));

        if (isCoreMapRoom(tileId)) {
            lore.add("§7바닥 노선: " + mapRouteName(tileId));
        }

        lore.add("");
        lore.add("§7인접 구역:");
        for (TileId adjacent : ship.generatedMap().adjacent(tileId)) {
            lore.add(
                    connectionColor(connectionState(tileId, adjacent))
                            + "- "
                            + ship.tileDisplayName(adjacent)
                            + " §8["
                            + connectionStateName(connectionState(tileId, adjacent))
                            + "]"
            );
        }

        inventory.setItem(
                slot,
                item(
                        schematicMaterial(tileId, definition.category()),
                        prefix + definition.displayName(),
                        lore
                )
        );
    }

    private static ItemStack mapLine(Material material, String name) {
        return item(material, name, List.of("§7실제 함선 바닥의 색 노선과 같습니다."));
    }

    private TileId mapTargetTile() {
        var ship = plugin.shipState().snapshot();
        if (ship.hull() < ReturnRequirements.developmentDefaults().minHull()) {
            TileId breach = plugin.hullBreachService()
                    .primaryUnrepairedTile()
                    .orElse(null);
            if (breach != null) {
                return breach;
            }
        }

        return new TileId(currentProblem().targetFacility().value());
    }

    private static boolean isCoreMapRoom(TileId tileId) {
        return switch (tileId.value()) {
            case "bridge", "engineering", "medical", "research", "cargo", "habitation" -> true;
            default -> false;
        };
    }

    private static String mapRouteName(TileId tileId) {
        return switch (tileId.value()) {
            case "bridge" -> "§b청록";
            case "engineering" -> "§c빨강";
            case "habitation" -> "§a초록";
            case "cargo" -> "§6주황";
            case "medical" -> "§d분홍";
            case "research" -> "§5보라";
            default -> "§7없음";
        };
    }

    private static Material schematicMaterial(
            TileId tileId,
            com.hushkisses.spacesurvival.map.tile.TileCategory category
    ) {
        return switch (tileId.value()) {
            case "bridge" -> Material.LIGHT_BLUE_CONCRETE;
            case "engineering" -> Material.RED_CONCRETE;
            case "habitation" -> Material.LIME_CONCRETE;
            case "cargo" -> Material.ORANGE_CONCRETE;
            case "medical" -> Material.PINK_CONCRETE;
            case "research" -> Material.PURPLE_CONCRETE;
            case "junction_1" -> Material.YELLOW_CONCRETE;
            default -> switch (category) {
                case AIRLOCK -> Material.CYAN_CONCRETE;
                case AUXILIARY -> Material.GREEN_CONCRETE;
                case CORE -> Material.LIGHT_BLUE_CONCRETE;
                case JUNCTION -> Material.YELLOW_CONCRETE;
                case CORRIDOR -> Material.WHITE_CONCRETE;
            };
        };
    }

    private ItemStack routeSummaryItem(
            Player player,
            com.hushkisses.spacesurvival.paper.map.physical.PhysicalShipSnapshot ship
    ) {
        PublicProblem problem = currentProblem();
        String target = plugin.facilityRegistry()
                .require(problem.targetFacility())
                .definition()
                .displayName();

        var plan = plugin.shipRouteService()
                .routeToFacility(player, problem.targetFacility())
                .orElse(null);

        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7현재 팀 우선 목표까지의 추천 경로입니다.");
        lore.add("§7목표: §e" + target);
        lore.add("");

        if (plan == null) {
            lore.add("§c현재 위치에서 경로를 계산할 수 없습니다.");
            lore.add("§7방 입구와 통로 목적지 표지를 확인하십시오.");
        } else if (plan.arrived()) {
            lore.add("§a이미 목표 시설에 도착했습니다.");
        } else {
            int step = 0;
            for (var tileId : plan.path()) {
                if (step == 0) {
                    lore.add("§a현재 §f" + ship.tileDisplayName(tileId));
                } else {
                    lore.add("§7" + step + ". §f" + ship.tileDisplayName(tileId));
                }
                step++;
            }

            if (!plan.routeUsable()) {
                lore.add("");
                lore.add("§e추천 경로 중 현재 통과할 수 없는 통로가 있습니다.");
                if (!plan.nextConnectionUsable()) {
                    lore.add("§c바로 다음 통로가 차단되어 있습니다.");
                }
                lore.add("§7통로 복구 또는 조건 충족 후 계속 이동하십시오.");
            }
        }

        return item(
                Material.COMPASS,
                "§b추천 경로 → " + target,
                lore
        );
    }

    private com.hushkisses.spacesurvival.map.connection.ConnectionState connectionState(
            com.hushkisses.spacesurvival.map.tile.TileId first,
            com.hushkisses.spacesurvival.map.tile.TileId second
    ) {
        for (var snapshot : plugin.physicalConnectionController().snapshots()) {
            var connection = snapshot.connection();
            var a = connection.first().tileId();
            var b = connection.second().tileId();
            if ((a.equals(first) && b.equals(second))
                    || (a.equals(second) && b.equals(first))) {
                return snapshot.state();
            }
        }
        return com.hushkisses.spacesurvival.map.connection.ConnectionState.DISABLED;
    }

    private static Material tileMaterial(
            com.hushkisses.spacesurvival.map.tile.TileCategory category
    ) {
        return switch (category) {
            case CORE -> Material.LIGHT_BLUE_CONCRETE;
            case CORRIDOR -> Material.WHITE_CONCRETE;
            case JUNCTION -> Material.YELLOW_CONCRETE;
            case AIRLOCK -> Material.ORANGE_CONCRETE;
            case AUXILIARY -> Material.LIME_CONCRETE;
        };
    }

    private static String tileCategoryName(
            com.hushkisses.spacesurvival.map.tile.TileCategory category
    ) {
        return switch (category) {
            case CORE -> "핵심 시설";
            case CORRIDOR -> "연결 통로";
            case JUNCTION -> "교차 구역";
            case AIRLOCK -> "에어록";
            case AUXILIARY -> "보조 구역";
        };
    }

    private static String connectionStateName(
            com.hushkisses.spacesurvival.map.connection.ConnectionState state
    ) {
        return switch (state) {
            case OPEN -> "개방";
            case POWER_REQUIRED -> "전력 필요";
            case KEYCARD_REQUIRED -> "키카드 필요";
            case LOCKED -> "잠김";
            case DISABLED -> "차단";
        };
    }

    private static String connectionColor(
            com.hushkisses.spacesurvival.map.connection.ConnectionState state
    ) {
        return switch (state) {
            case OPEN -> "§a";
            case POWER_REQUIRED, KEYCARD_REQUIRED -> "§e";
            case LOCKED, DISABLED -> "§c";
        };
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isPda(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        open(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isPda(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage("§e승무원 PDA는 버릴 수 없습니다.");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isPda);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (plugin.lobbyService().contains(PlayerId.of(player.getUniqueId()))) {
                ensurePda(player);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            PlayerId id = PlayerId.of(player.getUniqueId());
            if (plugin.lobbyService().contains(id)
                    && plugin.lobbyService().gameSession().isPresent()
                    && plugin.roleSelectionService().selectedRole(id).isPresent()) {
                ensurePda(player);
            }
        });
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        boolean mapInventory = event.getInventory().getHolder() instanceof MapInventoryHolder;
        if (!PERSONAL_TITLE.equals(title)
                && !PUBLIC_TITLE.equals(title)
                && !HELP_TITLE.equals(title)
                && !MAP_TITLE.equals(title)
                && !mapInventory) {
            return;
        }

        event.setCancelled(true);
        String action = readAction(event.getCurrentItem());
        if (action == null) return;

        switch (action) {
            case "personal" -> open(player);
            case "public" -> openPublic(player);
            case "help" -> openHelp(player);
            case "map" -> openMap(player);
            default -> {
            }
        }
    }

    private ItemStack situationItem() {
        String briefing = plugin.matchOrchestrator().setupSnapshot()
                .map(snapshot -> snapshot.scenario().definition().publicBriefing())
                .orElse("현재 공개 사고 브리핑을 불러올 수 없습니다.");

        return item(
                Material.WRITABLE_BOOK,
                "§c공개 사고 상황",
                List.of(
                        "§f" + briefing,
                        "",
                        "§b공통 목표: §f우주선을 복구하고 귀환 절차를 완료하십시오."
                )
        );
    }

    private ItemStack roleItem(RoleDefinition role) {
        if (role == null) {
            return item(Material.BARRIER, "§c직업 미선택", List.of("§7먼저 직업을 선택하십시오."));
        }

        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7" + role.description());
        lore.add("");
        lore.add("§f고급 행동:");
        for (RoleCapability capability : role.capabilities()) {
            lore.add("§d- " + capabilityName(capability));
        }

        return item(Material.NAME_TAG, "§e" + role.displayName(), lore);
    }

    private ItemStack equipmentItem(Player player, RoleId roleId) {
        if (roleId == null) {
            return item(Material.BARRIER, "§c시작 장비 미정", List.of("§7직업을 먼저 선택하십시오."));
        }

        FunctionalItemType type = starterEquipment(roleId);
        boolean has = plugin.functionalItemService().has(player, type);
        boolean active = plugin.matchOrchestrator().status()
                == com.hushkisses.spacesurvival.paper.match.MatchLifecycleStatus.ACTIVE;

        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7" + equipmentPurpose(type));
        lore.add("");
        lore.add("§7현재 보유: " + (has ? "§a보유" : "§c없음"));
        if (!active && !has) {
            lore.add("§8모든 승무원의 직업 선택이 끝나면 지급됩니다.");
        }

        return item(type.fallbackMaterial(), "§b시작 장비 — " + stripColor(type.displayName()), lore);
    }

    private ItemStack objectiveItem(
            String name,
            ObjectiveInstance objective,
            Material material
    ) {
        if (objective == null) {
            return item(
                    material,
                    name,
                    List.of("§7현재 배정된 목표가 없습니다.")
            );
        }

        return item(
                material,
                name + " §f— " + objective.definition().title(),
                List.of(
                        "§7" + objective.definition().description(),
                        "",
                        "§7진행도: §f" + objective.progress()
                                + "/" + objective.definition().targetProgress(),
                        "§7상태: §f" + objectiveStatusName(objective.status())
                )
        );
    }

    private ItemStack playerStatusItem(PlayerId playerId) {
        boolean alive = plugin.lobbyService().playerState(playerId)
                .map(state -> state.isAlive())
                .orElse(false);
        InfectionStage infection = plugin.infectionService().state(playerId).stage();
        PlayerSanctionState sanctions = plugin.sanctionStateRegistry().state(playerId);

        ArrayList<String> lore = new ArrayList<>();
        lore.add("§7생존 상태: " + (alive ? "§a생존" : "§c사망"));
        lore.add("§7감염 정보: §f" + visibleInfection(infection));
        lore.add("");
        lore.add("§7현재 제재/제한:");
        List<String> sanctionLines = sanctionLines(sanctions);
        if (sanctionLines.isEmpty()) {
            lore.add("§a- 없음");
        } else {
            sanctionLines.forEach(line -> lore.add("§c- " + line));
        }

        return item(Material.SHIELD, "§f개인 상태", lore);
    }

    private ItemStack locationItem(Player player) {
        String area = shipWorldService.activeSnapshot()
                .flatMap(snapshot -> snapshot.tileAt(player.getLocation())
                        .map(snapshot::tileDisplayName))
                .orElse("함선 외부/미배치");

        return item(
                Material.RECOVERY_COMPASS,
                "§f현재 위치",
                List.of("§b" + area)
        );
    }

    private ItemStack nextActionItem() {
        PublicProblem problem = currentProblem();
        String facility = plugin.facilityRegistry()
                .require(problem.targetFacility())
                .definition().displayName();

        return item(
                Material.TARGET,
                "§6다음 추천 행동",
                List.of(
                        "§f" + problem.title(),
                        "§7이동: §e" + facility,
                        "§7필요: §f" + problem.need(),
                        "§7" + problem.nextAction()
                )
        );
    }

    private PublicProblem currentProblem() {
        return guidanceResolver.resolve(
                plugin.returnObjectiveService().stage(),
                plugin.shipState().snapshot(),
                plugin.facilityRegistry().snapshots()
        );
    }

    private List<String> sharedResourceLore() {
        ArrayList<String> lore = new ArrayList<>();
        for (ResourceType type : ResourceType.values()) {
            lore.add("§7" + resourceName(type) + ": §f"
                    + plugin.resourceLedger().shared().quantity(type));
        }
        return lore;
    }

    private ItemStack createPda() {
        ItemStack item = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b승무원 PDA §7[우클릭]");
        meta.setLore(List.of(
                "§7개인 역할·목표·장비·현재 위치와",
                "§7팀의 다음 행동을 언제든 확인합니다.",
                "§8이 정보는 다른 플레이어에게 자동 공개되지 않습니다."
        ));
        meta.getPersistentDataContainer().set(pdaKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean hasPda(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isPda(item)) return true;
        }
        return false;
    }

    private boolean isPda(ItemStack item) {
        return item != null
                && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(pdaKey, PersistentDataType.BYTE);
    }

    private ItemStack actionItem(
            Material material,
            String name,
            String action,
            List<String> lore
    ) {
        ItemStack item = item(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private String readAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(actionKey, PersistentDataType.STRING);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static FunctionalItemType starterEquipment(RoleId roleId) {
        if (roleId.equals(DefaultRoleCatalog.ENGINEER)) return FunctionalItemType.ENGINEERING_MULTITOOL;
        if (roleId.equals(DefaultRoleCatalog.MEDIC)) return FunctionalItemType.MEDICAL_SCANNER;
        if (roleId.equals(DefaultRoleCatalog.SECURITY)) return FunctionalItemType.SECURITY_KEYCARD;
        if (roleId.equals(DefaultRoleCatalog.RESEARCHER)) return FunctionalItemType.RESEARCH_SCANNER;
        if (roleId.equals(DefaultRoleCatalog.NAV_COMMS)) return FunctionalItemType.RADIO;
        if (roleId.equals(DefaultRoleCatalog.CARGO_MAINTENANCE)) return FunctionalItemType.CARGO_SCANNER;
        throw new IllegalArgumentException("Unknown role: " + roleId);
    }

    private static String equipmentPurpose(FunctionalItemType type) {
        return switch (type) {
            case ENGINEERING_MULTITOOL -> "정밀 진단·전력 재배분·고급 수리에 사용합니다.";
            case MEDICAL_SCANNER -> "정밀 감염 검사·고급 치료·감염 억제에 사용합니다.";
            case SECURITY_KEYCARD -> "보안 통로와 보안 집행 기능에 사용합니다.";
            case RESEARCH_SCANNER -> "정밀 생체 분석·외계 생명체 연구·사건 원인 분석에 사용합니다.";
            case RADIO -> "항법 목적지 설정과 장거리 통신 기능에 사용합니다.";
            case CARGO_SCANNER -> "정밀 재고·희귀 자원 판별·고효율 가공에 사용합니다.";
        };
    }

    private static String capabilityName(RoleCapability capability) {
        return switch (capability) {
            case DIAGNOSE_FAULT -> "정밀 고장 진단";
            case ADVANCED_REPAIR -> "고급 수리";
            case REDISTRIBUTE_POWER -> "전력 재배분";
            case PRECISE_INFECTION_TEST -> "감염 정밀 검사";
            case ADVANCED_TREATMENT -> "고급 치료";
            case SUPPRESS_INFECTION -> "감염 억제";
            case EXECUTE_DISARM -> "무장해제 집행";
            case EXECUTE_DETENTION -> "감금 집행";
            case USE_SECURITY_SYSTEM -> "보안 시스템 사용";
            case EMERGENCY_LIMITED_PVP -> "비상사태 제한적 전투 권한";
            case PRECISE_BIO_ANALYSIS -> "정밀 생체 분석";
            case RESEARCH_ALIEN_LIFE -> "외계 생명체 연구";
            case IDENTIFY_EVENT_CAUSE -> "사건 원인 분석";
            case PRECISE_NAVIGATION_DATA -> "정밀 항법 정보";
            case CHANGE_DESTINATION -> "목적지 변경";
            case LONG_RANGE_COMMUNICATION -> "장거리 통신";
            case DISTRESS_SIGNAL -> "구조 신호 운용";
            case PRECISE_INVENTORY_CHECK -> "정밀 재고 확인";
            case IDENTIFY_RARE_RESOURCE -> "희귀 자원 판별";
            case HIGH_EFFICIENCY_PROCESSING -> "고효율 가공";
        };
    }

    private static String objectiveStatusName(ObjectiveStatus status) {
        return switch (status) {
            case ACTIVE -> "진행 중";
            case COMPLETED -> "완료";
            case FAILED -> "실패";
        };
    }

    private static String visibleInfection(InfectionStage stage) {
        return switch (stage) {
            case SYMPTOMATIC -> "감염 의심 증상이 확인됩니다.";
            case SUPPRESSED -> "의료적 감염 억제 상태입니다.";
            case NONE, EXPOSED, LATENT -> "현재 확인 가능한 감염 증상이 없습니다.";
        };
    }

    private static List<String> sanctionLines(PlayerSanctionState state) {
        ArrayList<String> lines = new ArrayList<>();
        if (state.medicalCheckOrdered()) lines.add("의료 검사 명령");
        if (state.disarmed()) lines.add("무장해제");
        if (state.detained()) lines.add("감금 — 이동 제한");
        if (state.accessRestricted()) lines.add("시설/통로 접근 제한");
        if (state.ejected()) lines.add("추방");
        return lines;
    }

    private static Material facilityMaterial(
            com.hushkisses.spacesurvival.facility.FacilityStatus status
    ) {
        return switch (status) {
            case NORMAL -> Material.LIME_CONCRETE;
            case DAMAGED -> Material.YELLOW_CONCRETE;
            case OFFLINE -> Material.RED_CONCRETE;
            case QUARANTINED -> Material.PURPLE_CONCRETE;
        };
    }

    private static String facilityStatusName(
            com.hushkisses.spacesurvival.facility.FacilityStatus status
    ) {
        return switch (status) {
            case NORMAL -> "정상";
            case DAMAGED -> "손상";
            case OFFLINE -> "정지";
            case QUARANTINED -> "격리";
        };
    }

    private static String resourceName(ResourceType type) {
        return switch (type) {
            case REPAIR_PARTS -> "수리 부품";
            case CIRCUITS -> "회로판";
            case POWER_CELLS -> "전력 셀";
            case FUEL -> "연료";
            case MEDICAL_SUPPLIES -> "의료 물자";
            case BIO_SAMPLES -> "생체 샘플";
            case DATA_CORES -> "데이터 코어";
        };
    }

    private static final class MapInventoryHolder implements InventoryHolder {
        private Inventory inventory;

        private void bind(Inventory inventory) {
            this.inventory = Objects.requireNonNull(inventory, "inventory");
        }

        @Override
        public Inventory getInventory() {
            if (inventory == null) {
                throw new IllegalStateException("Map inventory is not bound yet");
            }
            return inventory;
        }
    }

    private static String stripColor(String value) {
        return value.replaceAll("§.", "");
    }
}
