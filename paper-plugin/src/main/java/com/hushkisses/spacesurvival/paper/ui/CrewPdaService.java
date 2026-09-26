package com.hushkisses.spacesurvival.paper.ui;

import com.hushkisses.spacesurvival.guidance.PlayerGuidanceResolver;
import com.hushkisses.spacesurvival.guidance.PublicProblem;
import com.hushkisses.spacesurvival.infection.InfectionStage;
import com.hushkisses.spacesurvival.objective.ObjectiveInstance;
import com.hushkisses.spacesurvival.objective.ObjectiveSlot;
import com.hushkisses.spacesurvival.objective.ObjectiveStatus;
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
                        "§7금색/노란색/빨간색 연결 패드는",
                        "§7다른 함선 모듈로 이동하는 통로입니다.",
                        "§7막힌 통로는 상태나 권한 조건을 확인하십시오."
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
                "§f직업 장비",
                List.of(
                        "§7고급 행동에는 직업 권한뿐 아니라",
                        "§7실제 장비 소지가 필요한 경우가 있습니다.",
                        "§7장비를 잃으면 PDA에서 보유 여부를 다시 확인하십시오."
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

        Inventory inventory = Bukkit.createInventory(null, 54, MAP_TITLE);
        var current = ship.tileAt(player.getLocation()).orElse(null);
        var bridge = new com.hushkisses.spacesurvival.map.tile.TileId("bridge");

        int slot = 0;
        for (var tileId : ship.generatedMap().tileIds()) {
            if (slot >= 45) break;

            var definition = ship.definitions().get(tileId);
            if (definition == null) continue;

            ArrayList<String> lore = new ArrayList<>();
            lore.add("§7분류: §f" + tileCategoryName(definition.category()));
            lore.add("§7함교 거리: §f" + ship.generatedMap().distance(tileId, bridge) + "칸");
            if (tileId.equals(current)) {
                lore.add("§a현재 위치");
            }
            lore.add("");
            lore.add("§7연결:");

            for (var adjacent : ship.generatedMap().adjacent(tileId)) {
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
                    slot++,
                    item(
                            tileMaterial(definition.category()),
                            (tileId.equals(current) ? "§a▶ " : "§f")
                                    + definition.displayName(),
                            lore
                    )
            );
        }

        inventory.setItem(49, actionItem(
                Material.ARROW,
                "§a개인 정보로 돌아가기",
                "personal",
                List.of("§a클릭")
        ));
        player.openInventory(inventory);
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
        if (!PERSONAL_TITLE.equals(title)
                && !PUBLIC_TITLE.equals(title)
                && !HELP_TITLE.equals(title)
                && !MAP_TITLE.equals(title)) {
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

    private static String stripColor(String value) {
        return value.replaceAll("§.", "");
    }
}
