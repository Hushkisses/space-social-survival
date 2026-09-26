package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.facility.action.*;
import com.hushkisses.spacesurvival.guidance.PlayerGuidanceResolver;
import com.hushkisses.spacesurvival.guidance.PublicProblem;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.role.RoleCapability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class FacilityMenuService implements Listener {

    private static final String TITLE_PREFIX = "§8시설 작업 — ";

    private final SpaceSurvivalPlugin plugin;
    private final FacilityActionExecutor executor;
    private final PlayerGuidanceResolver guidanceResolver = new PlayerGuidanceResolver();

    public FacilityMenuService(
            SpaceSurvivalPlugin plugin,
            FacilityActionExecutor executor
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    public void open(Player player, FacilityId facilityId) {
        var facility = plugin.facilityRegistry().require(facilityId);
        List<FacilityActionDefinition> actions =
                plugin.facilityActionRegistry().forFacility(facilityId);

        int size = 36;
        FacilityMenuHolder holder = new FacilityMenuHolder(
                facilityId,
                size,
                TITLE_PREFIX + facility.definition().displayName()
        );
        Inventory inventory = holder.getInventory();

        inventory.setItem(0, statusItem(
                facility.definition().displayName(),
                facility.definition().description(),
                facility.status()
        ));
        inventory.setItem(1, metricsItem(facilityId));
        inventory.setItem(2, problemItem(facilityId));
        inventory.setItem(3, sharedResourcesItem(facilityId));
        inventory.setItem(4, carriedResourcesItem(player, facilityId));
        inventory.setItem(5, equipmentSummaryItem(player, actions));
        inventory.setItem(8, item(
                Material.KNOWLEDGE_BOOK,
                "§b작업 안내",
                List.of(
                        "§7초록/기본 아이콘: 현재 실행 가능",
                        "§7별 아이콘: 직업 고급 기능",
                        "§7장벽 아이콘: 조건 부족",
                        "§8소지 자원은 화물실 입고 후 공용 자원으로 사용됩니다."
                )
        ));

        int slot = 18;
        for (FacilityActionDefinition action : actions) {
            if (slot >= size) break;

            FacilityActionAccessDecision access = executor.access(player, action);
            inventory.setItem(slot, actionItem(player, action, access));
            holder.bind(slot, action.id());
            slot++;
        }

        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FacilityMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) {
            return;
        }

        FacilityActionId actionId = holder.actionAt(event.getRawSlot());
        if (actionId == null) {
            return;
        }

        FacilityActionDefinition action = plugin.facilityActionRegistry()
                .find(actionId)
                .orElse(null);
        if (action == null) {
            player.sendMessage("§c[시설] §f시설 기능을 찾을 수 없습니다.");
            return;
        }

        FacilityId facilityId = holder.facilityId();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            FacilityActionExecutionResult result = executor.execute(player, action);
            player.sendMessage(
                    (result.success() ? "§a[시설] §f" : "§c[시설] §f")
                            + result.message()
            );
            plugin.matchHudService().flashAction(
                    player,
                    result.success(),
                    result.message()
            );

            if (!plugin.meetingGuiService().hasActiveVote()) {
                open(player, facilityId);
            }
        });
    }

    private ItemStack metricsItem(FacilityId facilityId) {
        var ship = plugin.shipState().snapshot();
        List<String> lore = switch (facilityId.value()) {
            case "engineering" -> List.of(
                    "§7전력: §f" + ship.power() + "%",
                    "§7선체: §f" + ship.hull() + "%",
                    "§7원자로: §f" + ship.reactor() + "%"
            );
            case "medical" -> List.of(
                    "§7산소: §f" + ship.oxygen() + "%",
                    "§8개인 환자 상태는 의료 행동 결과로 확인합니다."
            );
            case "cargo" -> List.of(
                    "§7전력: §f" + ship.power() + "%",
                    "§8공용 재고와 소지 자원을 아래에서 비교하십시오."
            );
            case "research" -> List.of(
                    "§7전력: §f" + ship.power() + "%",
                    "§7산소: §f" + ship.oxygen() + "%"
            );
            case "habitation" -> List.of(
                    "§7산소: §f" + ship.oxygen() + "%"
            );
            default -> List.of(
                    "§7전력: §f" + ship.power() + "%",
                    "§7산소: §f" + ship.oxygen() + "%",
                    "§7선체: §f" + ship.hull() + "%",
                    "§7원자로: §f" + ship.reactor() + "%"
            );
        };

        return item(Material.REDSTONE, "§f관련 함선 수치", lore);
    }

    private ItemStack problemItem(FacilityId facilityId) {
        PublicProblem problem = guidanceResolver.resolve(
                plugin.returnObjectiveService().stage(),
                plugin.shipState().snapshot(),
                plugin.facilityRegistry().snapshots()
        );

        if (problem.targetFacility().equals(facilityId)) {
            return item(
                    Material.REDSTONE_TORCH,
                    "§c현재 우선 문제",
                    List.of(
                            "§f" + problem.title(),
                            "§7필요: §f" + problem.need(),
                            "§7" + problem.nextAction()
                    )
            );
        }

        return item(
                Material.LIME_DYE,
                "§a현재 팀 우선 문제는 다른 시설",
                List.of(
                        "§7대상: §f" + plugin.facilityRegistry()
                                .require(problem.targetFacility()).definition().displayName(),
                        "§7" + problem.title()
                )
        );
    }

    private ItemStack sharedResourcesItem(FacilityId facilityId) {
        ArrayList<String> lore = new ArrayList<>();
        for (ResourceType type : relevantResources(facilityId)) {
            lore.add("§7" + resourceName(type) + ": §f"
                    + plugin.resourceLedger().shared().quantity(type));
        }
        if (lore.isEmpty()) {
            lore.add("§7이 시설의 기본 정보 기능에는 공용 자원이 필요하지 않습니다.");
        }
        return item(Material.CHEST, "§e공용 자원", lore);
    }

    private ItemStack carriedResourcesItem(Player player, FacilityId facilityId) {
        Map<ResourceType, Integer> carried = carriedResources(player);
        ArrayList<String> lore = new ArrayList<>();
        for (ResourceType type : relevantResources(facilityId)) {
            lore.add("§7" + resourceName(type) + ": §f" + carried.getOrDefault(type, 0));
        }
        if (facilityId.value().equals("cargo")) {
            lore.add("");
            lore.add("§a[소지 자원 공용 창고 입고]로 모두 입고할 수 있습니다.");
        } else {
            lore.add("");
            lore.add("§8시설 소비는 공용 창고 기준입니다.");
        }
        return item(Material.BUNDLE, "§6내가 소지한 관련 자원", lore);
    }

    private ItemStack equipmentSummaryItem(
            Player player,
            List<FacilityActionDefinition> actions
    ) {
        LinkedHashSet<FunctionalItemType> required = new LinkedHashSet<>();
        for (FacilityActionDefinition action : actions) {
            FunctionalItemType type = executor.requiredEquipment(action);
            if (type != null) required.add(type);
        }

        ArrayList<String> lore = new ArrayList<>();
        if (required.isEmpty()) {
            lore.add("§7이 시설의 현재 기능에는 별도 직업 장비 조건이 없습니다.");
        } else {
            for (FunctionalItemType type : required) {
                lore.add((plugin.functionalItemService().has(player, type) ? "§a" : "§c")
                        + stripColor(type.displayName())
                        + " — "
                        + (plugin.functionalItemService().has(player, type) ? "보유" : "없음"));
            }
        }
        return item(Material.IRON_PICKAXE, "§b필요 직업 장비", lore);
    }

    private ItemStack actionItem(
            Player player,
            FacilityActionDefinition action,
            FacilityActionAccessDecision access
    ) {
        List<Requirement> requirements = requirements(action.id().value());
        boolean resourcesReady = requirements.stream().allMatch(requirement ->
                plugin.resourceLedger().shared()
                        .has(requirement.type(), requirement.amount())
        );

        Material material;
        if (!access.allowed() || !resourcesReady) {
            material = Material.BARRIER;
        } else if (action.tier() == FacilityActionTier.ADVANCED) {
            material = Material.NETHER_STAR;
        } else {
            material = Material.IRON_INGOT;
        }

        ArrayList<String> lore = new ArrayList<>();
        lore.add(action.tier() == FacilityActionTier.ADVANCED
                ? "§d직업 고급 기능"
                : "§7기본 기능");

        action.requiredCapabilityOptional().ifPresent(capability ->
                lore.add("§7필요 전문성: §f" + capabilityName(capability))
        );

        FunctionalItemType equipment = executor.requiredEquipment(action);
        if (equipment != null) {
            boolean has = plugin.functionalItemService().has(player, equipment);
            lore.add("§7필요 장비: "
                    + (has ? "§a" : "§c")
                    + stripColor(equipment.displayName())
                    + (has ? " (보유)" : " (없음)"));
        }

        for (Requirement requirement : requirements) {
            int available = plugin.resourceLedger().shared().quantity(requirement.type());
            lore.add("§7필요 자원: §f" + resourceName(requirement.type())
                    + " " + requirement.amount()
                    + " §8(공용 " + available + ")");
        }

        lore.add("§7예상 효과: §f" + effectPreview(action.id().value()));
        lore.add("");

        if (!access.allowed()) {
            lore.add("§c사용 불가: " + denialName(access.denialReason()));
        } else if (!resourcesReady) {
            lore.add("§c사용 불가: 공용 자원이 부족합니다.");
        } else {
            lore.add("§a클릭하여 실행");
        }

        return item(material, "§f" + action.displayName(), lore);
    }

    private Map<ResourceType, Integer> carriedResources(Player player) {
        EnumMap<ResourceType, Integer> result = new EnumMap<>(ResourceType.class);
        for (ItemStack stack : player.getInventory().getContents()) {
            ResourceType type = plugin.resourcePhysicalItemService().typeOf(stack).orElse(null);
            if (type != null) {
                result.merge(type, stack.getAmount(), Integer::sum);
            }
        }
        return result;
    }

    private static List<ResourceType> relevantResources(FacilityId facilityId) {
        return switch (facilityId.value()) {
            case "engineering" -> List.of(
                    ResourceType.POWER_CELLS,
                    ResourceType.FUEL,
                    ResourceType.REPAIR_PARTS
            );
            case "medical" -> List.of(ResourceType.MEDICAL_SUPPLIES);
            case "research" -> List.of(ResourceType.BIO_SAMPLES, ResourceType.DATA_CORES);
            case "cargo" -> List.of(ResourceType.values());
            case "habitation" -> List.of(ResourceType.MEDICAL_SUPPLIES, ResourceType.REPAIR_PARTS);
            default -> List.of();
        };
    }

    private static List<Requirement> requirements(String id) {
        return switch (id) {
            case "engineering.power" -> List.of(new Requirement(ResourceType.POWER_CELLS, 1));
            case "engineering.engine" -> List.of(new Requirement(ResourceType.FUEL, 1));
            case "engineering.repair", "engineering.oxygen" ->
                    List.of(new Requirement(ResourceType.REPAIR_PARTS, 1));
            case "engineering.advanced_repair" -> List.of(new Requirement(ResourceType.REPAIR_PARTS, 2));
            case "medical.treat", "medical.clear_status", "medical.decontaminate",
                    "medical.advanced_treatment", "medical.suppress_infection" ->
                    List.of(new Requirement(ResourceType.MEDICAL_SUPPLIES, 1));
            case "research.sample" -> List.of(new Requirement(ResourceType.BIO_SAMPLES, 1));
            case "cargo.process" -> List.of(new Requirement(ResourceType.REPAIR_PARTS, 2));
            case "cargo.repair" -> List.of(new Requirement(ResourceType.REPAIR_PARTS, 1));
            case "cargo.efficient_process" -> List.of(
                    new Requirement(ResourceType.REPAIR_PARTS, 1),
                    new Requirement(ResourceType.CIRCUITS, 1)
            );
            default -> List.of();
        };
    }

    private static String effectPreview(String id) {
        return switch (id) {
            case "bridge.objective" -> "현재 공통 귀환 단계 확인";
            case "bridge.ship_status" -> "전력·산소·선체·원자로 상태 확인";
            case "bridge.meeting" -> "일반 회의 소집";
            case "bridge.return" -> "현재 귀환 단계 진행 또는 최종 유지 시작";
            case "bridge.destination" -> "항법 목적지 확정";
            case "bridge.long_range_comms" -> "장거리 통신 계통 정상화";
            case "engineering.power" -> "전력 +15";
            case "engineering.engine" -> "원자로/엔진 안정도 +10";
            case "engineering.repair" -> "선체 안정도 +12, 기관실 손상 복구 가능";
            case "engineering.oxygen" -> "수리 부품 1개를 사용해 산소 +15";
            case "engineering.diagnose" -> "정밀 함선 상태 진단";
            case "engineering.redistribute" -> "전력 +8";
            case "engineering.advanced_repair" -> "선체 +25, 원자로 +15, 고장 통로 복구";
            case "medical.treat" -> "현재 플레이어 기본 치료";
            case "medical.clear_status" -> "현재 플레이어 상태이상 1개 제거";
            case "medical.infection_test" -> "현재 검사 정밀도 범위의 감염 검사";
            case "medical.decontaminate" -> "의료 물자 1개를 사용해 의료실 손상/오염 상태 정상화";
            case "medical.precise_test" -> "정밀 감염 검사";
            case "medical.advanced_treatment" -> "현재 플레이어 고급 치료";
            case "medical.suppress_infection" -> "확인된 감염 진행 억제";
            case "research.sample" -> "생체 샘플 1개 분석 후 데이터 코어 1개 확보";
            case "research.data" -> "공용 데이터 코어 수량 확인";
            case "research.alien_material" -> "공개 가능한 외계물질 흔적 분석";
            case "research.precise_bio" -> "정밀 생체 분석";
            case "research.alien_life" -> "외계 생명체 활동 신호 분석";
            case "research.event_cause" -> "최근 사건 원인 정보 분석";
            case "cargo.store", "cargo.sort", "cargo.inventory" -> "공용 재고 확인";
            case "cargo.deposit" -> "소지한 물리 자원을 공용 창고에 입고";
            case "cargo.process" -> "수리 부품 2 → 회로판 1";
            case "cargo.repair" -> "수리 부품 1개를 사용해 화물실 손상 상태 정상화";
            case "cargo.rare" -> "희귀 자원 재고 확인";
            case "cargo.efficient_process" -> "수리 부품 1 + 회로판 1 → 전력 셀 1";
            case "habitation.supply" -> "공용 보급품 1개를 물리 아이템으로 수령";
            case "habitation.maintenance" -> "개인 장비 정비";
            case "habitation.locker" -> "개인 보관 자원 확인";
            default -> "실행 결과를 시설에서 확인";
        };
    }

    private static ItemStack statusItem(
            String name,
            String description,
            FacilityStatus status
    ) {
        Material material = switch (status) {
            case NORMAL -> Material.LIME_CONCRETE;
            case DAMAGED -> Material.YELLOW_CONCRETE;
            case OFFLINE -> Material.RED_CONCRETE;
            case QUARANTINED -> Material.PURPLE_CONCRETE;
        };

        return item(
                material,
                "§f" + name + " §7— " + statusName(status),
                List.of("§7" + description)
        );
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String statusName(FacilityStatus status) {
        return switch (status) {
            case NORMAL -> "§a정상";
            case DAMAGED -> "§e손상";
            case OFFLINE -> "§c정지";
            case QUARANTINED -> "§5격리";
        };
    }

    private static String denialName(FacilityActionAccessDecision.DenialReason reason) {
        return switch (reason) {
            case FACILITY_OFFLINE -> "시설이 정지 상태입니다.";
            case FACILITY_QUARANTINED -> "시설이 격리 상태입니다.";
            case FACILITY_DAMAGED_ADVANCED_UNAVAILABLE -> "시설 손상으로 고급 기능이 잠겼습니다.";
            case MISSING_ROLE_CAPABILITY -> "현재 직업에 필요한 전문 행동 권한이 없습니다.";
            case MISSING_REQUIRED_EQUIPMENT -> "필요한 직업 장비를 소지하고 있지 않습니다.";
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
            case USE_SECURITY_SYSTEM -> "보안 시스템";
            case EMERGENCY_LIMITED_PVP -> "비상 제한 전투";
            case PRECISE_BIO_ANALYSIS -> "정밀 생체 분석";
            case RESEARCH_ALIEN_LIFE -> "외계 생명체 연구";
            case IDENTIFY_EVENT_CAUSE -> "사건 원인 분석";
            case PRECISE_NAVIGATION_DATA -> "정밀 항법 정보";
            case CHANGE_DESTINATION -> "목적지 변경";
            case LONG_RANGE_COMMUNICATION -> "장거리 통신";
            case DISTRESS_SIGNAL -> "구조 신호";
            case PRECISE_INVENTORY_CHECK -> "정밀 재고 확인";
            case IDENTIFY_RARE_RESOURCE -> "희귀 자원 판별";
            case HIGH_EFFICIENCY_PROCESSING -> "고효율 가공";
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

    private record Requirement(ResourceType type, int amount) {
    }
}
