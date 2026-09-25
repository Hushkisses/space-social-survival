package com.hushkisses.spacesurvival.paper.command;

import com.hushkisses.spacesurvival.event.GameEventScale;
import com.hushkisses.spacesurvival.map.connection.ConnectionState;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Objects;

public final class PlaytestIntegrationCommandHandler {

    private final SpaceSurvivalPlugin plugin;

    public PlaytestIntegrationCommandHandler(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean supports(String root) {
        return root.equalsIgnoreCase("structure")
                || root.equalsIgnoreCase("door")
                || root.equalsIgnoreCase("director");
    }

    public boolean handle(CommandSender sender, String[] args) {
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "structure" -> structure(sender, args);
            case "door" -> door(sender, args);
            case "director" -> director(sender, args);
            default -> false;
        };
    }

    private boolean structure(CommandSender sender, String[] args) {
        sender.sendMessage("§6[우주 생존] §f함선 NBT 모듈 상태");
        sender.sendMessage(
                "§7폴더: §f"
                        + plugin.shipWorldService()
                        .structureLoader()
                        .structuresDirectory()
                        .getAbsolutePath()
        );

        var placements = plugin.shipWorldService().structurePlacements();
        if (placements.isEmpty()) {
            sender.sendMessage("§7아직 생성된 물리 함선이 없습니다.");
            return true;
        }

        long loaded = placements.values().stream()
                .filter(result -> result.placed())
                .count();

        sender.sendMessage(
                "§7NBT 사용: §f" + loaded
                        + " / " + placements.size()
                        + " §8(나머지는 개발용 방 폴백)"
        );

        placements.forEach((tileId, result) -> sender.sendMessage(
                "§7- §f" + tileId.value()
                        + " §8→ "
                        + (result.placed() ? "§aNBT " + result.detail() : "§e폴백")
        ));
        return true;
    }

    private boolean door(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§6[우주 생존] §f물리 통로 상태");
            var snapshots = plugin.physicalConnectionController().snapshots();
            if (snapshots.isEmpty()) {
                sender.sendMessage("§7생성된 통로가 없습니다.");
                return true;
            }

            snapshots.forEach(connection -> sender.sendMessage(
                    "§7#" + connection.id()
                            + " §f" + connection.state()
                            + " §8"
                            + connection.connection().first().tileId().value()
                            + " ↔ "
                            + connection.connection().second().tileId().value()
            ));
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c통로 상태 변경 권한이 없습니다.");
            return true;
        }

        if (args[1].equalsIgnoreCase("repair")) {
            int count = plugin.physicalConnectionController().openAllFaultedConnections();
            plugin.gameEventRuntimeState().setFlag("door_fault", false);
            sender.sendMessage("§a통로 " + count + "개를 정상 상태로 복구했습니다.");
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage("§c사용법: /space door set <id> <open|locked|power_required|keycard_required|disabled>");
                return true;
            }

            int id;
            try {
                id = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c통로 ID는 정수여야 합니다.");
                return true;
            }

            ConnectionState state;
            try {
                state = ConnectionState.valueOf(args[3].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                sender.sendMessage("§c알 수 없는 통로 상태입니다.");
                return true;
            }

            if (!plugin.physicalConnectionController().setState(id, state)) {
                sender.sendMessage("§c해당 통로 ID가 없습니다.");
                return true;
            }

            sender.sendMessage("§a통로 #" + id + " 상태를 " + state + "로 변경했습니다.");
            return true;
        }

        sender.sendMessage("§c사용법: /space door list");
        sender.sendMessage("§c사용법: /space door set <id> <state>");
        sender.sendMessage("§c사용법: /space door repair");
        return true;
    }

    private boolean director(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            var snapshot = plugin.incidentDirector().snapshot();
            long elapsed = plugin.gameRuntimeService().snapshot()
                    .map(value -> value.time().elapsed().toSeconds())
                    .orElse(0L);

            sender.sendMessage("§6[우주 생존] §fIncident Director");
            sender.sendMessage("§7실행 중: §f" + snapshot.running());
            sender.sendMessage("§7현재 경과: §f" + elapsed + "초");
            sender.sendMessage("§7다음 소형 사건: §f" + formatAt(snapshot.nextSmallAtSeconds()));
            sender.sendMessage("§7다음 대형 사건: §f" + formatAt(snapshot.nextMajorAtSeconds()));
            sender.sendMessage("§7대형 사건 발생 수: §f" + snapshot.majorEventsTriggered());
            sender.sendMessage("§7최근 사건: §f" + (snapshot.lastEventId() == null ? "없음" : snapshot.lastEventId()));
            return true;
        }

        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c사건 디렉터 제어 권한이 없습니다.");
            return true;
        }

        if (args[1].equalsIgnoreCase("force")) {
            if (args.length < 3) {
                sender.sendMessage("§c사용법: /space director force <small|large>");
                return true;
            }

            GameEventScale scale = switch (args[2].toLowerCase(Locale.ROOT)) {
                case "small" -> GameEventScale.SMALL;
                case "large", "major" -> GameEventScale.LARGE;
                default -> null;
            };
            if (scale == null) {
                sender.sendMessage("§c사건 규모는 small 또는 large여야 합니다.");
                return true;
            }

            var event = plugin.incidentDirector().force(scale);
            sender.sendMessage("§a사건을 즉시 발생시켰습니다: " + event.displayName());
            return true;
        }

        sender.sendMessage("§c사용법: /space director status");
        sender.sendMessage("§c사용법: /space director force <small|large>");
        return true;
    }

    private static String formatAt(long seconds) {
        if (seconds == Long.MAX_VALUE) return "없음";
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
