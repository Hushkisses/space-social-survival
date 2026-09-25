package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.social.meeting.*;
import com.hushkisses.spacesurvival.social.sanction.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;

public final class SocialCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public SocialCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("meeting")
                || root.equalsIgnoreCase("sanction")
                || root.equalsIgnoreCase("pvp");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "meeting" -> meeting(sender, args);
            case "sanction" -> sanction(sender, args);
            case "pvp" -> pvp(sender, args);
            default -> false;
        };
    }

    private boolean meeting(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMeetingUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "start" -> startRegularMeeting(sender);
            case "emergency" -> startEmergencyMeeting(sender, args);
            case "status" -> meetingStatus(sender);
            case "end" -> endMeeting(sender);
            default -> {
                sendMeetingUsage(sender);
                yield true;
            }
        };
    }

    private boolean startRegularMeeting(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c회의 시작 권한이 없습니다.");
            return true;
        }

        MeetingStartResult result = plugin.meetingGuiService().startRegular();
        if (!result.started()) {
            sender.sendMessage("§c회의를 시작할 수 없습니다: " + denialName(result.denialReason()));
        }
        return true;
    }

    private boolean startEmergencyMeeting(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c긴급회의 시작 권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space meeting emergency <body|infection|reactor|security|event>");
            return true;
        }

        EmergencyMeetingReason reason = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "body" -> EmergencyMeetingReason.BODY_FOUND;
            case "infection" -> EmergencyMeetingReason.INFECTION_ALERT;
            case "reactor" -> EmergencyMeetingReason.REACTOR_CRITICAL;
            case "security" -> EmergencyMeetingReason.SECURITY_ALERT;
            case "event" -> EmergencyMeetingReason.SPECIAL_EVENT;
            default -> null;
        };

        if (reason == null) {
            sender.sendMessage("§c알 수 없는 긴급회의 사유입니다.");
            return true;
        }

        MeetingStartResult result = plugin.meetingGuiService().startEmergency(reason);
        if (!result.started()) {
            sender.sendMessage("§c긴급회의를 시작할 수 없습니다: " + denialName(result.denialReason()));
        }
        return true;
    }

    private boolean meetingStatus(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f회의 상태");
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);

        if (meeting == null) {
            sender.sendMessage("§7활성 회의: §e없음");
            sender.sendMessage(
                    "§7일반 회의 쿨타임: §f"
                            + plugin.meetingService().remainingCooldown().toSeconds()
                            + "초"
            );
            return true;
        }

        sender.sendMessage("§7종류: §f" + meeting.type());
        meeting.emergencyReason().ifPresent(
                reason -> sender.sendMessage("§7사유: §f" + emergencyReasonName(reason))
        );
        sender.sendMessage("§7참가자 수: §f" + meeting.participants().size());
        sender.sendMessage("§7투표 수: §f" + plugin.meetingGuiService().voteCount());
        return true;
    }

    private boolean endMeeting(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c회의 종료 권한이 없습니다.");
            return true;
        }

        if (plugin.meetingService().activeMeeting().isEmpty()) {
            sender.sendMessage("§c활성 회의가 없습니다.");
            return true;
        }

        plugin.meetingGuiService().cancel();
        return true;
    }

    private boolean sanction(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendSanctionUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "vote" -> sanctionVote(sender, args);
            case "resolve" -> sanctionResolve(sender);
            case "execute" -> sanctionExecute(sender);
            case "status" -> sanctionStatus(sender, args);
            default -> {
                sendSanctionUsage(sender);
                yield true;
            }
        };
    }

    private boolean sanctionVote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c투표는 게임 안의 플레이어만 할 수 있습니다.");
            return true;
        }
        if (!plugin.meetingGuiService().hasActiveVote()) {
            sender.sendMessage("§c활성 회의가 없습니다.");
            return true;
        }
        if (args.length < 3) {
            sendSanctionUsage(sender);
            return true;
        }

        SanctionType type;
        try {
            type = SanctionType.valueOf(args[2].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c처분: no_action, medical_check, disarm, detain, access_restrict, eject");
            return true;
        }

        SanctionChoice choice;
        if (type == SanctionType.NO_ACTION) {
            choice = SanctionChoice.noAction();
        } else {
            if (args.length < 4) {
                sender.sendMessage("§c대상 플레이어 이름이 필요합니다.");
                return true;
            }
            Player target = plugin.getServer().getPlayerExact(args[3]);
            if (target == null) {
                sender.sendMessage("§c접속 중인 대상 플레이어를 찾을 수 없습니다.");
                return true;
            }
            choice = new SanctionChoice(type, PlayerId.of(target.getUniqueId()));
        }

        try {
            plugin.meetingGuiService().vote(player, choice);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            sender.sendMessage("§c투표할 수 없습니다: " + exception.getMessage());
        }
        return true;
    }

    private boolean sanctionResolve(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c투표 집계 권한이 없습니다.");
            return true;
        }

        try {
            SanctionVoteResult result = plugin.meetingGuiService().resolveNow();
            sender.sendMessage(
                    "§6[투표 결과] §f"
                            + result.winningChoice().sanction().name()
                            + (result.tied() ? " §e(동률 → 무조치)" : "")
            );
        } catch (IllegalStateException exception) {
            sender.sendMessage("§c투표를 집계할 수 없습니다: " + exception.getMessage());
        }
        return true;
    }

    private boolean sanctionExecute(CommandSender sender) {
        sender.sendMessage("§7PT-008부터 회의 처분은 투표 집계와 동시에 자동 집행됩니다.");
        return true;
    }

    private boolean sanctionStatus(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space sanction status <player>");
            return true;
        }

        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage("§c접속 중인 대상 플레이어를 찾을 수 없습니다.");
            return true;
        }

        PlayerSanctionState state = plugin.sanctionStateRegistry()
                .state(PlayerId.of(target.getUniqueId()));

        sender.sendMessage("§6[우주 생존] §f처분 상태: " + target.getName());
        sender.sendMessage("§7의료검사 명령: §f" + state.medicalCheckOrdered());
        sender.sendMessage("§7무장해제: §f" + state.disarmed());
        sender.sendMessage("§7감금: §f" + state.detained());
        sender.sendMessage("§7출입제한: §f" + state.accessRestricted());
        sender.sendMessage("§7추방: §f" + state.ejected());
        return true;
    }

    private boolean pvp(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage("§6[우주 생존] §f조건부 PvP 상태");
            sender.sendMessage("§7비상사태: §f" + plugin.pvpRuntimeState().emergencyDeclared());
            sender.sendMessage("§7시나리오 허용: §f" + plugin.pvpRuntimeState().scenarioAllows());
            sender.sendMessage("§7특수사건 허용: §f" + plugin.pvpRuntimeState().specialEventAllows());
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§cPvP 상태 변경 권한이 없습니다.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space pvp <emergency|scenario|special> <on|off>");
            return true;
        }

        boolean value;
        if (args[2].equalsIgnoreCase("on")) value = true;
        else if (args[2].equalsIgnoreCase("off")) value = false;
        else {
            sender.sendMessage("§c값은 on 또는 off여야 합니다.");
            return true;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "emergency" -> plugin.pvpRuntimeState().setEmergencyDeclared(value);
            case "scenario" -> plugin.pvpRuntimeState().setScenarioAllows(value);
            case "special" -> plugin.pvpRuntimeState().setSpecialEventAllows(value);
            default -> {
                sender.sendMessage("§c항목은 emergency, scenario, special 중 하나여야 합니다.");
                return true;
            }
        }

        sender.sendMessage("§aPvP 조건을 변경했습니다.");
        return true;
    }

    private static String denialName(MeetingStartDenialReason reason) {
        return switch (reason) {
            case MEETING_ALREADY_ACTIVE -> "이미 회의 진행 중";
            case BRIDGE_UNAVAILABLE -> "함교 사용 불가";
            case NO_POWER_OR_COMMUNICATION -> "전력 및 통신 사용 불가";
            case COOLDOWN_ACTIVE -> "회의 쿨타임";
            case NO_PARTICIPANTS -> "참가자 없음";
        };
    }

    private static String emergencyReasonName(EmergencyMeetingReason reason) {
        return switch (reason) {
            case BODY_FOUND -> "시체 발견";
            case INFECTION_ALERT -> "감염 경보";
            case REACTOR_CRITICAL -> "원자로 중대사고";
            case SECURITY_ALERT -> "보안 경보";
            case SPECIAL_EVENT -> "특수 사건";
        };
    }

    private static void sendMeetingUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space meeting start");
        sender.sendMessage("§c사용법: /space meeting emergency <body|infection|reactor|security|event>");
        sender.sendMessage("§c사용법: /space meeting status");
        sender.sendMessage("§c사용법: /space meeting end");
    }

    private static void sendSanctionUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space sanction vote <type> [player]");
        sender.sendMessage("§c사용법: /space sanction resolve");
        sender.sendMessage("§c사용법: /space sanction execute");
        sender.sendMessage("§c사용법: /space sanction status <player>");
    }
}
