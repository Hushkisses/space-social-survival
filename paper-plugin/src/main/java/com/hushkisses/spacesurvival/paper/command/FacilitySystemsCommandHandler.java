package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.facility.action.FacilityActionDefinition;
import com.hushkisses.spacesurvival.facility.engineering.EngineeringDiagnosis;
import com.hushkisses.spacesurvival.facility.medical.MedicalCondition;
import com.hushkisses.spacesurvival.facility.medical.PatientMedicalState;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;

public final class FacilitySystemsCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public FacilitySystemsCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("engineering")
                || root.equalsIgnoreCase("medical")
                || root.equalsIgnoreCase("actions");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "engineering" -> handleEngineering(sender, args);
            case "medical" -> handleMedical(sender, args);
            case "actions" -> handleActions(sender, args);
            default -> false;
        };
    }

    private boolean handleEngineering(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            EngineeringDiagnosis diagnosis = plugin.engineeringFacilityService().diagnose();
            sender.sendMessage("§6[우주 생존] §f기관실 진단");
            sender.sendMessage("§7시설 상태: §f" + statusName(diagnosis.facilityStatus()));
            sender.sendMessage("§7전력: §f" + diagnosis.shipState().power() + "%");
            sender.sendMessage("§7선체: §f" + diagnosis.shipState().hull() + "%");
            sender.sendMessage("§7원자로: §f" + diagnosis.shipState().reactor() + "%");
            return true;
        }

        if (args[1].equalsIgnoreCase("adjust")) {
            if (!sender.hasPermission("spacesurvival.admin")) {
                sender.sendMessage("§c기관실 디버그 조정 권한이 없습니다.");
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage("§c사용법: /space engineering adjust <power|hull|reactor> <delta>");
                return true;
            }

            ShipMetric metric;
            try {
                metric = ShipMetric.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage("§c항목은 power, hull, reactor 중 하나여야 합니다.");
                return true;
            }

            int delta;
            try {
                delta = Integer.parseInt(args[3]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c변화량은 정수여야 합니다.");
                return true;
            }

            try {
                int value = plugin.engineeringFacilityService().adjust(metric, delta);
                sender.sendMessage("§a기관실 작업 완료: " + metric.name() + "=" + value + "%");
            } catch (IllegalArgumentException | IllegalStateException exception) {
                sender.sendMessage("§c기관실 작업을 수행할 수 없습니다: " + exception.getMessage());
            }
            return true;
        }

        sender.sendMessage("§c사용법: /space engineering status");
        sender.sendMessage("§c사용법: /space engineering adjust <power|hull|reactor> <delta>");
        return true;
    }

    private boolean handleMedical(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c의료 디버그 명령은 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId());
        PatientMedicalState state = plugin.medicalFacilityService().patient(playerId);

        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage("§6[우주 생존] §f개인 의료 상태");
            sender.sendMessage("§7건강도: §f" + state.healthPercent() + "%");
            sender.sendMessage("§7상태이상: §f" + (state.conditions().isEmpty() ? "없음" : state.conditions()));
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c의료 상태 변경 권한이 없습니다.");
            return true;
        }

        if (args[1].equalsIgnoreCase("damage")) {
            if (args.length < 3) {
                sender.sendMessage("§c사용법: /space medical damage <amount>");
                return true;
            }
            int amount = parsePositive(sender, args[2]);
            if (amount < 0) return true;
            state.setHealthPercent(Math.max(0, state.healthPercent() - amount));
            state.addCondition(MedicalCondition.WOUNDED);
            sender.sendMessage("§e부상 상태를 적용했습니다. 건강도: " + state.healthPercent() + "%");
            return true;
        }

        if (args[1].equalsIgnoreCase("treat")) {
            if (args.length < 3) {
                sender.sendMessage("§c사용법: /space medical treat <amount>");
                return true;
            }
            int amount = parsePositive(sender, args[2]);
            if (amount < 0) return true;
            try {
                int health = plugin.medicalFacilityService().treat(playerId, amount);
                sender.sendMessage("§a치료 완료. 건강도: " + health + "%");
            } catch (IllegalStateException exception) {
                sender.sendMessage("§c의료실을 사용할 수 없습니다.");
            }
            return true;
        }

        if (args[1].equalsIgnoreCase("condition") && args.length >= 4) {
            MedicalCondition condition;
            try {
                condition = MedicalCondition.valueOf(args[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage("§c상태는 wounded, contaminated, exhausted 중 하나여야 합니다.");
                return true;
            }

            if (args[2].equalsIgnoreCase("add")) {
                state.addCondition(condition);
                sender.sendMessage("§e상태이상을 추가했습니다: " + condition.name());
            } else if (args[2].equalsIgnoreCase("clear")) {
                try {
                    plugin.medicalFacilityService().clearCondition(playerId, condition);
                    sender.sendMessage("§a상태이상을 제거했습니다: " + condition.name());
                } catch (IllegalStateException exception) {
                    sender.sendMessage("§c의료실을 사용할 수 없습니다.");
                }
            } else {
                sender.sendMessage("§c사용법: /space medical condition <add|clear> <condition>");
            }
            return true;
        }

        sender.sendMessage("§c사용법: /space medical status");
        sender.sendMessage("§c사용법: /space medical damage <amount>");
        sender.sendMessage("§c사용법: /space medical treat <amount>");
        sender.sendMessage("§c사용법: /space medical condition <add|clear> <condition>");
        return true;
    }

    private boolean handleActions(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c사용법: /space actions <facilityId>");
            return true;
        }

        FacilityId facilityId = new FacilityId(args[1].toLowerCase(Locale.ROOT));
        if (plugin.facilityRegistry().find(facilityId).isEmpty()) {
            sender.sendMessage("§c존재하지 않는 시설 ID입니다.");
            return true;
        }

        sender.sendMessage("§6[우주 생존] §f시설 기능 목록");
        for (FacilityActionDefinition action : plugin.facilityActionRegistry().forFacility(facilityId)) {
            String capability = action.requiredCapability() == null
                    ? ""
                    : " §8필요능력=" + action.requiredCapability().name();
            sender.sendMessage(
                    "§7- §f" + action.displayName()
                            + " §8[" + action.tier().name() + "]"
                            + capability
            );
        }
        return true;
    }

    private static int parsePositive(CommandSender sender, String value) {
        try {
            int amount = Integer.parseInt(value);
            if (amount < 1) throw new NumberFormatException();
            return amount;
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c값은 1 이상의 정수여야 합니다.");
            return -1;
        }
    }

    private static String statusName(FacilityStatus status) {
        return switch (status) {
            case NORMAL -> "정상";
            case DAMAGED -> "손상";
            case OFFLINE -> "정지";
            case QUARANTINED -> "격리";
        };
    }
}
