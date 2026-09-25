package com.hushkisses.spacesurvival.paper.facility;

import com.hushkisses.spacesurvival.ending.FinalHoldStatus;
import com.hushkisses.spacesurvival.ending.ReturnStage;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.facility.action.*;
import com.hushkisses.spacesurvival.facility.medical.MedicalCondition;
import com.hushkisses.spacesurvival.infection.InfectionTestResult;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.item.FunctionalItemType;
import com.hushkisses.spacesurvival.paper.map.physical.PhysicalConnectionController;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.resource.ResourceStore;
import com.hushkisses.spacesurvival.resource.ResourceType;
import com.hushkisses.spacesurvival.resource.processing.ProcessingRecipeId;
import com.hushkisses.spacesurvival.resource.processing.ProcessingResult;
import com.hushkisses.spacesurvival.role.RoleCapability;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import com.hushkisses.spacesurvival.social.meeting.MeetingStartResult;
import org.bukkit.entity.Player;

import java.util.*;

public final class FacilityActionExecutor {

    private final SpaceSurvivalPlugin plugin;
    private final PhysicalConnectionController connections;
    private final FacilityActionAccessPolicy accessPolicy = new FacilityActionAccessPolicy();

    public FacilityActionExecutor(
            SpaceSurvivalPlugin plugin,
            PhysicalConnectionController connections
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    public FacilityActionAccessDecision access(
            Player player,
            FacilityActionDefinition action
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");

        FacilityStatus status = plugin.facilityRegistry()
                .require(action.facilityId())
                .status();

        FacilityActionAccessDecision coreDecision =
                accessPolicy.evaluate(status, action, capabilities(player));
        if (!coreDecision.allowed()) {
            return coreDecision;
        }

        FunctionalItemType requiredItem = requiredEquipment(action.id().value());
        if (requiredItem != null
                && !plugin.functionalItemService().has(player, requiredItem)) {
            return FacilityActionAccessDecision.deny(
                    FacilityActionAccessDecision.DenialReason.MISSING_REQUIRED_EQUIPMENT
            );
        }

        return FacilityActionAccessDecision.allow();
    }

    public FacilityActionExecutionResult execute(
            Player player,
            FacilityActionDefinition action
    ) {
        FacilityActionAccessDecision decision = access(player, action);
        if (!decision.allowed()) {
            return FacilityActionExecutionResult.failure(denialMessage(decision.denialReason()));
        }

        try {
            return executeAllowed(player, action.id().value());
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return FacilityActionExecutionResult.failure(exception.getMessage());
        }
    }

    private FacilityActionExecutionResult executeAllowed(Player player, String id) {
        return switch (id) {
            case "bridge.objective" -> result(
                    "공통 목표 단계: " + plugin.returnObjectiveService().stage()
            );
            case "bridge.ship_status" -> {
                var ship = plugin.shipState().snapshot();
                yield result(
                        "전력 " + ship.power()
                                + "% / 산소 " + ship.oxygen()
                                + "% / 선체 " + ship.hull()
                                + "% / 원자로 " + ship.reactor() + "%"
                );
            }
            case "bridge.meeting" -> startMeeting();
            case "bridge.return" -> advanceReturn();
            case "bridge.destination" -> plugin.returnObjectiveService().markNavigationReady()
                    ? result("항법 목적지를 확정했습니다. 귀환 준비 단계로 이동합니다.")
                    : failure("현재는 목적지를 확정할 단계가 아닙니다.");
            case "bridge.long_range_comms" -> {
                plugin.radioRuntimeState().setCommunicationsOutage(false);
                plugin.radioRuntimeState().setLongRangeEnabled(true);
                yield result("장거리 통신 계통을 정상화했습니다.");
            }

            case "engineering.power" -> engineeringResource(
                    ResourceType.POWER_CELLS,
                    ShipMetric.POWER,
                    15,
                    "전력 셀을 투입해 전력을 복구했습니다."
            );
            case "engineering.engine" -> engineeringResource(
                    ResourceType.FUEL,
                    ShipMetric.REACTOR,
                    10,
                    "연료를 투입해 원자로/엔진 출력을 안정화했습니다."
            );
            case "engineering.repair" -> {
                if (!shared().remove(ResourceType.REPAIR_PARTS, 1)) {
                    yield failure("공용 수리 부품이 부족합니다.");
                }
                int value = plugin.engineeringFacilityService().adjust(ShipMetric.HULL, 12);
                if (plugin.facilityRegistry().require(actionFacility("engineering")).status()
                        == FacilityStatus.DAMAGED) {
                    plugin.facilityRegistry().require(actionFacility("engineering"))
                            .setStatus(FacilityStatus.NORMAL);
                }
                yield result("핵심 수리를 완료했습니다. 선체 안정도: " + value + "%");
            }
            case "engineering.diagnose" -> {
                var diagnosis = plugin.engineeringFacilityService().diagnose();
                yield result(
                        "정밀 진단 — 전력 " + diagnosis.shipState().power()
                                + "% / 선체 " + diagnosis.shipState().hull()
                                + "% / 원자로 " + diagnosis.shipState().reactor() + "%"
                );
            }
            case "engineering.redistribute" -> {
                int value = plugin.engineeringFacilityService().adjust(ShipMetric.POWER, 8);
                yield result("전력을 재배분했습니다. 전력: " + value + "%");
            }
            case "engineering.advanced_repair" -> {
                if (!shared().remove(ResourceType.REPAIR_PARTS, 2)) {
                    yield failure("고급 수리에 필요한 수리 부품 2개가 부족합니다.");
                }
                int hull = plugin.engineeringFacilityService().adjust(ShipMetric.HULL, 25);
                int reactor = plugin.engineeringFacilityService().adjust(ShipMetric.REACTOR, 15);
                plugin.facilityRegistry().require(actionFacility("engineering"))
                        .setStatus(FacilityStatus.NORMAL);
                int doors = connections.openAllFaultedConnections();
                plugin.gameEventRuntimeState().setFlag("door_fault", false);
                yield result(
                        "고급 수리 완료 — 선체 " + hull
                                + "% / 원자로 " + reactor
                                + "% / 복구된 통로 " + doors + "개"
                );
            }

            case "medical.treat" -> medicalTreat(player, 25, 1);
            case "medical.clear_status" -> clearMedicalCondition(player);
            case "medical.infection_test" -> infectionTest(player, false);
            case "medical.precise_test" -> infectionTest(player, true);
            case "medical.advanced_treatment" -> medicalTreat(player, 50, 1);
            case "medical.suppress_infection" -> suppressInfection(player);

            case "research.sample" -> {
                if (!shared().remove(ResourceType.BIO_SAMPLES, 1)) {
                    yield failure("분석할 생체 샘플이 없습니다.");
                }
                shared().add(ResourceType.DATA_CORES, 1);
                yield result("생체 샘플을 분석해 데이터 코어 1개를 확보했습니다.");
            }
            case "research.data" -> result(
                    "공용 데이터 코어: " + shared().quantity(ResourceType.DATA_CORES)
            );
            case "research.alien_material" -> result(
                    plugin.gameEventRuntimeState().hasFlag("alien_intrusion")
                            ? "외계 생물 침입과 일치하는 흔적이 확인됩니다."
                            : "현재 확인 가능한 외계 생물 침입 흔적은 없습니다."
            );
            case "research.precise_bio" -> infectionTest(player, true);
            case "research.alien_life" -> result(
                    plugin.gameEventRuntimeState().hasFlag("alien_intrusion")
                            ? "외계 생물 활동 신호가 감지됩니다."
                            : "활성 외계 생물 신호가 없습니다."
            );
            case "research.event_cause" -> {
                var history = plugin.gameEventRuntimeState().history();
                yield history.isEmpty()
                        ? result("분석할 사건 기록이 없습니다.")
                        : result("최근 사건 식별자: " + history.getLast().eventId().value());
            }

            case "cargo.store", "cargo.sort", "cargo.inventory" -> result(
                    "공용 재고: " + shared().snapshot()
            );
            case "cargo.deposit" -> depositCarriedResources(player);
            case "cargo.process" -> process("circuit_salvage");
            case "cargo.rare" -> result(
                    "희귀 자원 — 생체 샘플 "
                            + shared().quantity(ResourceType.BIO_SAMPLES)
                            + " / 데이터 코어 "
                            + shared().quantity(ResourceType.DATA_CORES)
            );
            case "cargo.efficient_process" -> process("power_cell_refurbish");

            case "habitation.supply" -> transferSupply(player);
            case "habitation.maintenance" -> {
                if (!shared().remove(ResourceType.REPAIR_PARTS, 1)) {
                    yield failure("정비에 사용할 수리 부품이 없습니다.");
                }
                yield result("개인 장비 정비를 완료했습니다.");
            }
            case "habitation.locker" -> result(
                    "개인 보관 자원: "
                            + plugin.resourceLedger()
                            .personal(PlayerId.of(player.getUniqueId()))
                            .snapshot()
            );

            default -> failure("아직 실제 동작이 연결되지 않은 시설 기능입니다: " + id);
        };
    }

    private FacilityActionExecutionResult startMeeting() {
        MeetingStartResult meeting = plugin.meetingService().startRegular(
                plugin.lobbyService().snapshot().players(),
                plugin.radioRuntimeState().longRangeAvailable()
        );
        if (!meeting.started()) {
            return failure("회의를 시작할 수 없습니다: " + meeting.denialReason());
        }
        plugin.getServer().broadcastMessage("§6[회의] §f함교에서 일반 회의가 소집되었습니다.");
        return result("회의를 소집했습니다.");
    }

    private FacilityActionExecutionResult advanceReturn() {
        ReturnStage stage = plugin.returnObjectiveService().stage();

        return switch (stage) {
            case SURVIVAL_SYSTEMS -> plugin.returnObjectiveService().advanceSurvivalSystems()
                    ? result("생존 기반 복구가 확인되었습니다. 항법 단계로 이동합니다.")
                    : failure("전력·산소·선체·원자로 또는 핵심 시설 조건이 부족합니다.");
            case NAVIGATION -> failure("먼저 항법 담당 기능으로 목적지를 확정해야 합니다.");
            case RETURN_PREPARATION -> plugin.returnObjectiveService().markReturnPreparationReady()
                    ? result("귀환 준비를 완료했습니다. 최종 버티기 단계가 열렸습니다.")
                    : failure("귀환 준비를 완료할 수 없습니다.");
            case FINAL_HOLD -> {
                if (plugin.finalHoldService().status() != FinalHoldStatus.NOT_STARTED) {
                    yield failure("최종 버티기가 이미 시작되었거나 종료되었습니다.");
                }
                plugin.endingRuntimeService().startFinalHold();
                yield result("최종 귀환 유지 절차를 시작했습니다.");
            }
            case COMPLETED -> result("귀환 절차가 이미 완료되었습니다.");
            case FAILED -> failure("귀환 절차가 실패 상태입니다.");
        };
    }

    private FacilityActionExecutionResult engineeringResource(
            ResourceType resource,
            ShipMetric metric,
            int amount,
            String success
    ) {
        if (!shared().remove(resource, 1)) {
            return failure("필요 자원이 부족합니다: " + resource.name());
        }
        int value = plugin.engineeringFacilityService().adjust(metric, amount);
        return result(success + " 현재 " + metric.name() + "=" + value + "%");
    }

    private FacilityActionExecutionResult medicalTreat(
            Player player,
            int amount,
            int supplies
    ) {
        if (!shared().remove(ResourceType.MEDICAL_SUPPLIES, supplies)) {
            return failure("공용 의료 물자가 부족합니다.");
        }

        PlayerId id = PlayerId.of(player.getUniqueId());
        int health = plugin.medicalFacilityService().treat(id, amount);
        double minecraftHealth = Math.min(
                player.getMaxHealth(),
                player.getHealth() + player.getMaxHealth() * (amount / 100.0)
        );
        player.setHealth(minecraftHealth);
        return result("치료를 완료했습니다. 의료 건강도: " + health + "%");
    }

    private FacilityActionExecutionResult clearMedicalCondition(Player player) {
        PlayerId id = PlayerId.of(player.getUniqueId());
        var patient = plugin.medicalFacilityService().patient(id);

        MedicalCondition condition = patient.conditions().stream().findFirst().orElse(null);
        if (condition == null) {
            return result("제거할 상태이상이 없습니다.");
        }
        if (!shared().remove(ResourceType.MEDICAL_SUPPLIES, 1)) {
            return failure("공용 의료 물자가 부족합니다.");
        }

        plugin.medicalFacilityService().clearCondition(id, condition);
        return result("상태이상을 제거했습니다: " + condition.name());
    }

    private FacilityActionExecutionResult infectionTest(Player player, boolean precise) {
        InfectionTestResult test = plugin.infectionService().test(
                PlayerId.of(player.getUniqueId()),
                precise
        );
        return result((precise ? "정밀" : "기본") + " 감염 검사 결과: " + test.name());
    }

    private FacilityActionExecutionResult suppressInfection(Player player) {
        PlayerId id = PlayerId.of(player.getUniqueId());
        if (!plugin.infectionService().state(id).infected()) {
            return result("현재 감염 상태가 아닙니다.");
        }
        if (!shared().remove(ResourceType.MEDICAL_SUPPLIES, 1)) {
            return failure("감염 억제에 필요한 의료 물자가 부족합니다.");
        }

        plugin.infectionService().suppress(id);
        return result("감염 진행을 억제했습니다.");
    }

    private FacilityActionExecutionResult process(String recipeId) {
        var recipe = plugin.processingRegistry()
                .find(new ProcessingRecipeId(recipeId))
                .orElseThrow();

        ProcessingResult result = plugin.processingService().process(shared(), recipe);
        return result == ProcessingResult.SUCCESS
                ? result("가공 완료: " + recipe.displayName())
                : failure("가공에 필요한 공용 자원이 부족합니다.");
    }

    private FacilityActionExecutionResult transferSupply(Player player) {
        if (shared().remove(ResourceType.MEDICAL_SUPPLIES, 1)) {
            plugin.resourcePhysicalItemService().give(
                    player,
                    ResourceType.MEDICAL_SUPPLIES,
                    1
            );
            return result("공용 창고에서 의료 물자 1개를 꺼냈습니다.");
        }
        if (shared().remove(ResourceType.REPAIR_PARTS, 1)) {
            plugin.resourcePhysicalItemService().give(
                    player,
                    ResourceType.REPAIR_PARTS,
                    1
            );
            return result("공용 창고에서 수리 부품 1개를 꺼냈습니다.");
        }
        return failure("수령 가능한 공용 보급품이 없습니다.");
    }

    private FacilityActionExecutionResult depositCarriedResources(Player player) {
        Map<ResourceType, Integer> removed =
                plugin.resourcePhysicalItemService().removeAllFrom(player);

        if (removed.isEmpty()) {
            return failure("입고할 물리 자원 아이템이 없습니다.");
        }

        int total = 0;
        for (Map.Entry<ResourceType, Integer> entry : removed.entrySet()) {
            shared().add(entry.getKey(), entry.getValue());
            total += entry.getValue();
        }

        return result("공용 창고에 자원 " + total + "개를 입고했습니다: " + removed);
    }

    private ResourceStore shared() {
        return plugin.resourceLedger().shared();
    }

    private Set<RoleCapability> capabilities(Player player) {
        PlayerId id = PlayerId.of(player.getUniqueId());
        return plugin.roleSelectionService().selectedRole(id)
                .map(plugin.roleRegistry()::require)
                .map(role -> role.capabilities())
                .orElse(Set.of());
    }

    private static FunctionalItemType requiredEquipment(String actionId) {
        if (actionId.startsWith("engineering.")
                && (actionId.equals("engineering.diagnose")
                || actionId.equals("engineering.redistribute")
                || actionId.equals("engineering.advanced_repair"))) {
            return FunctionalItemType.ENGINEERING_MULTITOOL;
        }
        if (actionId.startsWith("medical.")
                && (actionId.equals("medical.precise_test")
                || actionId.equals("medical.advanced_treatment")
                || actionId.equals("medical.suppress_infection"))) {
            return FunctionalItemType.MEDICAL_SCANNER;
        }
        if (actionId.startsWith("research.")
                && (actionId.equals("research.precise_bio")
                || actionId.equals("research.alien_life")
                || actionId.equals("research.event_cause"))) {
            return FunctionalItemType.RESEARCH_SCANNER;
        }
        if (actionId.startsWith("cargo.")
                && (actionId.equals("cargo.inventory")
                || actionId.equals("cargo.rare")
                || actionId.equals("cargo.efficient_process"))) {
            return FunctionalItemType.CARGO_SCANNER;
        }
        if (actionId.equals("bridge.destination")
                || actionId.equals("bridge.long_range_comms")) {
            return FunctionalItemType.RADIO;
        }
        return null;
    }

    private static com.hushkisses.spacesurvival.facility.FacilityId actionFacility(String id) {
        return new com.hushkisses.spacesurvival.facility.FacilityId(id);
    }

    private static String denialMessage(FacilityActionAccessDecision.DenialReason reason) {
        return switch (reason) {
            case FACILITY_OFFLINE -> "시설이 정지 상태입니다.";
            case FACILITY_QUARANTINED -> "시설이 격리되어 사용할 수 없습니다.";
            case FACILITY_DAMAGED_ADVANCED_UNAVAILABLE -> "시설이 손상되어 고급 기능을 사용할 수 없습니다.";
            case MISSING_ROLE_CAPABILITY -> "현재 직업에는 이 고급 기능을 사용할 권한이 없습니다.";
            case MISSING_REQUIRED_EQUIPMENT -> "필요한 직업 장비를 소지하고 있지 않습니다.";
        };
    }

    private static FacilityActionExecutionResult result(String message) {
        return FacilityActionExecutionResult.success(message);
    }

    private static FacilityActionExecutionResult failure(String message) {
        return FacilityActionExecutionResult.failure(message);
    }
}
