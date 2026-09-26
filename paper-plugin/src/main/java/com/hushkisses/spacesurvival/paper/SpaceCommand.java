package com.hushkisses.spacesurvival.paper;

import com.hushkisses.spacesurvival.core.BootstrapMarker;
import com.hushkisses.spacesurvival.facility.FacilityId;
import com.hushkisses.spacesurvival.facility.FacilityStateSnapshot;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.lobby.LobbyJoinResult;
import com.hushkisses.spacesurvival.lobby.LobbyLeaveResult;
import com.hushkisses.spacesurvival.lobby.LobbySnapshot;
import com.hushkisses.spacesurvival.lobby.LobbyStartException;
import com.hushkisses.spacesurvival.paper.map.MapDebugService;
import com.hushkisses.spacesurvival.paper.command.EventCommandHandler;
import com.hushkisses.spacesurvival.paper.command.FacilitySystemsCommandHandler;
import com.hushkisses.spacesurvival.paper.command.ObjectiveCommandHandler;
import com.hushkisses.spacesurvival.paper.command.ResourceCommandHandler;
import com.hushkisses.spacesurvival.paper.command.SocialCommandHandler;
import com.hushkisses.spacesurvival.paper.command.CommunicationCommandHandler;
import com.hushkisses.spacesurvival.paper.command.ScenarioCommandHandler;
import com.hushkisses.spacesurvival.paper.command.MvpEndingCommandHandler;
import com.hushkisses.spacesurvival.paper.command.MatchCommandHandler;
import com.hushkisses.spacesurvival.paper.command.PlaytestIntegrationCommandHandler;
import com.hushkisses.spacesurvival.paper.runtime.GameRuntimeService;
import com.hushkisses.spacesurvival.time.CrisisStage;
import com.hushkisses.spacesurvival.ship.ShipMetric;
import com.hushkisses.spacesurvival.ship.ShipStateSnapshot;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.RoleDefinition;
import com.hushkisses.spacesurvival.role.RoleId;
import com.hushkisses.spacesurvival.role.selection.RoleCandidateSet;
import com.hushkisses.spacesurvival.role.selection.RoleSelectionResult;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

final class SpaceCommand implements CommandExecutor {

    private final SpaceSurvivalPlugin plugin;
    private final MapDebugService mapDebugService = new MapDebugService();
    private final FacilitySystemsCommandHandler facilitySystemsCommandHandler;
    private final ResourceCommandHandler resourceCommandHandler;
    private final ObjectiveCommandHandler objectiveCommandHandler;
    private final EventCommandHandler eventCommandHandler;
    private final SocialCommandHandler socialCommandHandler;
    private final CommunicationCommandHandler communicationCommandHandler;
    private final ScenarioCommandHandler scenarioCommandHandler;
    private final MvpEndingCommandHandler mvpEndingCommandHandler;
    private final MatchCommandHandler matchCommandHandler;
    private final PlaytestIntegrationCommandHandler playtestIntegrationCommandHandler;

    SpaceCommand(SpaceSurvivalPlugin plugin) {
        this.plugin = plugin;
        this.facilitySystemsCommandHandler = new FacilitySystemsCommandHandler(plugin);
        this.resourceCommandHandler = new ResourceCommandHandler(plugin);
        this.objectiveCommandHandler = new ObjectiveCommandHandler(plugin);
        this.eventCommandHandler = new EventCommandHandler(plugin);
        this.socialCommandHandler = new SocialCommandHandler(plugin);
        this.communicationCommandHandler = new CommunicationCommandHandler(plugin);
        this.scenarioCommandHandler = new ScenarioCommandHandler(plugin);
        this.mvpEndingCommandHandler = new MvpEndingCommandHandler(plugin);
        this.matchCommandHandler = new MatchCommandHandler(plugin);
        this.playtestIntegrationCommandHandler = new PlaytestIntegrationCommandHandler(plugin);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sendStatus(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("map")) {
            return handleMap(sender, args);
        }

        if (args[0].equalsIgnoreCase("lobby")) {
            return handleLobby(sender, args);
        }

        if (args[0].equalsIgnoreCase("role")) {
            return handleRole(sender, args);
        }

        if (args[0].equalsIgnoreCase("briefing")) {
            return handleBriefing(sender);
        }

        if (args[0].equalsIgnoreCase("runtime")) {
            return handleRuntime(sender, args);
        }

        if (args[0].equalsIgnoreCase("ship")) {
            return handleShip(sender, args);
        }

        if (args[0].equalsIgnoreCase("facility")) {
            return handleFacility(sender, args);
        }

        if (facilitySystemsCommandHandler.supports(args[0])) {
            return facilitySystemsCommandHandler.handle(sender, args);
        }

        if (args[0].equalsIgnoreCase("resource")) {
            return resourceCommandHandler.handle(sender, args);
        }

        if (args[0].equalsIgnoreCase("objective")) {
            return objectiveCommandHandler.handle(sender, args);
        }

        if (args[0].equalsIgnoreCase("event")) {
            return eventCommandHandler.handle(sender, args);
        }

        if (socialCommandHandler.supports(args[0])) {
            return socialCommandHandler.handle(sender, args);
        }

        if (communicationCommandHandler.supports(args[0])) {
            return communicationCommandHandler.handle(sender, args);
        }

        if (scenarioCommandHandler.supports(args[0])) {
            return scenarioCommandHandler.handle(sender, args);
        }

        if (mvpEndingCommandHandler.supports(args[0])) {
            return mvpEndingCommandHandler.handle(sender, args);
        }

        if (args[0].equalsIgnoreCase("match")) {
            return matchCommandHandler.handle(sender, args);
        }

        if (playtestIntegrationCommandHandler.supports(args[0])) {
            return playtestIntegrationCommandHandler.handle(sender, args);
        }

        sendUsage(sender);
        return true;
    }

    private boolean handleFacility(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendFacilityUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "list" -> {
                sender.sendMessage("§6[우주 생존] §f시설 목록");
                for (FacilityStateSnapshot snapshot : plugin.facilityRegistry().snapshots()) {
                    sender.sendMessage(
                            "§7- §f"
                                    + snapshot.displayName()
                                    + " §8("
                                    + snapshot.id()
                                    + ") §7: "
                                    + facilityStatusColor(snapshot.status())
                                    + facilityStatusName(snapshot.status())
                    );
                }
                yield true;
            }
            case "status" -> handleFacilityStatus(sender, args);
            case "set" -> handleFacilitySet(sender, args);
            case "reset" -> {
                if (!sender.hasPermission("spacesurvival.admin")) {
                    sender.sendMessage("§c시설 상태 초기화 권한이 없습니다.");
                    yield true;
                }
                plugin.facilityRegistry().resetAll();
                sender.sendMessage("§a모든 시설 상태를 정상으로 초기화했습니다.");
                yield true;
            }
            default -> {
                sendFacilityUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleFacilityStatus(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendFacilityUsage(sender);
            return true;
        }

        FacilityId id = new FacilityId(args[2].toLowerCase());
        FacilityStateSnapshot snapshot = plugin.facilityRegistry().find(id)
                .map(state -> state.snapshot())
                .orElse(null);

        if (snapshot == null) {
            sender.sendMessage("§c존재하지 않는 시설 ID입니다.");
            return true;
        }

        sender.sendMessage("§6[우주 생존] §f시설 상태");
        sender.sendMessage("§7시설: §f" + snapshot.displayName() + " §8(" + snapshot.id() + ")");
        sender.sendMessage(
                "§7상태: "
                        + facilityStatusColor(snapshot.status())
                        + facilityStatusName(snapshot.status())
        );
        return true;
    }

    private boolean handleFacilitySet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c시설 상태 변경 권한이 없습니다.");
            return true;
        }
        if (args.length < 4) {
            sendFacilityUsage(sender);
            return true;
        }

        FacilityId id = new FacilityId(args[2].toLowerCase());
        var facility = plugin.facilityRegistry().find(id).orElse(null);
        if (facility == null) {
            sender.sendMessage("§c존재하지 않는 시설 ID입니다.");
            return true;
        }

        FacilityStatus status;
        try {
            status = FacilityStatus.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c상태는 normal, damaged, offline, quarantined 중 하나여야 합니다.");
            return true;
        }

        facility.setStatus(status);
        sender.sendMessage(
                "§a시설 상태를 변경했습니다: "
                        + facility.definition().displayName()
                        + " -> "
                        + facilityStatusName(status)
        );
        return true;
    }

    private static String facilityStatusName(FacilityStatus status) {
        return switch (status) {
            case NORMAL -> "정상";
            case DAMAGED -> "손상";
            case OFFLINE -> "정지";
            case QUARANTINED -> "격리";
        };
    }

    private static String facilityStatusColor(FacilityStatus status) {
        return switch (status) {
            case NORMAL -> "§a";
            case DAMAGED -> "§e";
            case OFFLINE -> "§c";
            case QUARANTINED -> "§6";
        };
    }

    private boolean handleShip(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendShipUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "status" -> {
                sendShipStatus(sender);
                yield true;
            }
            case "reset" -> {
                if (!sender.hasPermission("spacesurvival.admin")) {
                    sender.sendMessage("§c우주선 상태 초기화 권한이 없습니다.");
                    yield true;
                }
                plugin.resetShipState();
                sender.sendMessage("§a우주선 상태를 정상 상태로 초기화했습니다.");
                sendShipStatus(sender);
                yield true;
            }
            case "set" -> handleShipSet(sender, args);
            default -> {
                sendShipUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleShipSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c우주선 상태 변경 권한이 없습니다.");
            return true;
        }
        if (args.length < 4) {
            sendShipUsage(sender);
            return true;
        }

        ShipMetric metric;
        try {
            metric = ShipMetric.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c항목은 power, oxygen, hull, reactor 중 하나여야 합니다.");
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[3]);
        } catch (NumberFormatException exception) {
            sender.sendMessage("§c값은 0~100 사이 정수여야 합니다.");
            return true;
        }

        try {
            plugin.shipState().set(metric, value);
        } catch (IllegalArgumentException exception) {
            sender.sendMessage("§c값은 0~100 사이여야 합니다.");
            return true;
        }

        sender.sendMessage("§a우주선 상태를 변경했습니다: " + metric.name() + "=" + value);
        sendShipStatus(sender);
        return true;
    }

    private void sendShipStatus(CommandSender sender) {
        ShipStateSnapshot ship = plugin.shipState().snapshot();
        sender.sendMessage("§6[우주 생존] §f우주선 핵심 상태");
        sender.sendMessage("§7전력: §f" + ship.power() + "%");
        sender.sendMessage("§7산소: §f" + ship.oxygen() + "%");
        sender.sendMessage("§7선체 안정도: §f" + ship.hull() + "%");
        sender.sendMessage("§7원자로 안정도: §f" + ship.reactor() + "%");
    }

    private boolean handleRuntime(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendRuntimeUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "start" -> {
                if (!sender.hasPermission("spacesurvival.admin")) {
                    sender.sendMessage("§c런타임 시작 권한이 없습니다.");
                    yield true;
                }

                try {
                    plugin.gameRuntimeService().start();
                    sender.sendMessage("§a게임 타이머를 시작했습니다.");
                    sendRuntimeStatus(sender);
                } catch (IllegalStateException exception) {
                    sender.sendMessage("§c게임 타이머가 이미 실행 중입니다.");
                }
                yield true;
            }
            case "stop" -> {
                if (!sender.hasPermission("spacesurvival.admin")) {
                    sender.sendMessage("§c런타임 종료 권한이 없습니다.");
                    yield true;
                }

                try {
                    plugin.gameRuntimeService().stop();
                    sender.sendMessage("§e게임 타이머를 정지했습니다.");
                    sendRuntimeStatus(sender);
                } catch (IllegalStateException exception) {
                    sender.sendMessage("§c시작된 게임 타이머가 없습니다.");
                }
                yield true;
            }
            case "status" -> {
                sendRuntimeStatus(sender);
                yield true;
            }
            default -> {
                sendRuntimeUsage(sender);
                yield true;
            }
        };
    }

    private void sendRuntimeStatus(CommandSender sender) {
        GameRuntimeService.RuntimeSnapshot snapshot = plugin.gameRuntimeService()
                .snapshot()
                .orElse(null);

        sender.sendMessage("§6[우주 생존] §f게임 런타임 상태");

        if (snapshot == null) {
            sender.sendMessage("§7타이머: §e시작 전");
            sender.sendMessage("§7위기 단계: §a안정");
            return;
        }

        long elapsedSeconds = snapshot.time().elapsed().toSeconds();
        long targetSeconds = snapshot.time().targetDuration().toSeconds();

        sender.sendMessage(
                "§7타이머: §f"
                        + formatDuration(Duration.ofSeconds(elapsedSeconds))
                        + " §7/ §f"
                        + formatDuration(Duration.ofSeconds(targetSeconds))
        );
        sender.sendMessage(
                "§7상태: "
                        + (snapshot.time().running() ? "§a진행 중" : "§e정지")
        );
        sender.sendMessage(
                "§7위기 단계: "
                        + crisisColor(snapshot.crisisStage())
                        + crisisName(snapshot.crisisStage())
        );
    }

    private static String formatDuration(Duration duration) {
        long seconds = duration.toSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }

    private static String crisisName(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> "안정";
            case ALERT -> "경계";
            case CRISIS -> "위기";
            case COLLAPSE -> "붕괴";
        };
    }

    private static String crisisColor(CrisisStage stage) {
        return switch (stage) {
            case STABLE -> "§a";
            case ALERT -> "§e";
            case CRISIS -> "§6";
            case COLLAPSE -> "§c";
        };
    }

    private boolean handleRole(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendRoleUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "prepare" -> handleRolePrepare(sender, args);
            case "candidates" -> handleRoleCandidates(sender);
            case "gui" -> handleRoleGui(sender);
            case "choose" -> handleRoleChoose(sender, args);
            case "status" -> {
                sendRoleStatus(sender);
                yield true;
            }
            default -> {
                sendRoleUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleRolePrepare(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c직업 후보 생성 권한이 없습니다.");
            return true;
        }

        LobbySnapshot snapshot = plugin.lobbyService().snapshot();
        if (snapshot.players().isEmpty()) {
            sender.sendMessage("§c대기실 참가자가 없습니다.");
            return true;
        }

        long seed;
        if (args.length >= 3) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c시드는 정수여야 합니다.");
                sender.sendMessage("§7사용법: /space role prepare [seed]");
                return true;
            }
        } else {
            seed = ThreadLocalRandom.current().nextLong();
        }

        try {
            plugin.roleSelectionService().prepareCandidates(
                    snapshot.players(),
                    new Random(seed)
            );
        } catch (IllegalStateException exception) {
            sender.sendMessage("§c이미 직업 선택이 시작되어 후보를 다시 만들 수 없습니다.");
            return true;
        }

        sender.sendMessage("§a직업 후보를 생성했습니다. 시드: " + seed);
        sender.sendMessage("§7대상 인원: " + snapshot.playerCount());

        for (PlayerId playerId : snapshot.players()) {
            Player online = plugin.getServer().getPlayer(playerId.value());
            if (online != null && online.isOnline()) {
                plugin.openingBriefingUi().open(online);
            }
        }

        return true;
    }

    private boolean handleBriefing(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        plugin.openingBriefingUi().open(player);
        return true;
    }

    private boolean handleRoleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        plugin.roleSelectionUi().open(player);
        return true;
    }

    private boolean handleRoleCandidates(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId());
        RoleCandidateSet set = plugin.roleSelectionService()
                .candidates(playerId)
                .orElse(null);

        if (set == null) {
            sender.sendMessage("§c아직 직업 후보가 생성되지 않았습니다.");
            return true;
        }

        sender.sendMessage("§6[우주 생존] §f직업 후보 3개");
        for (RoleId roleId : set.candidates()) {
            RoleDefinition role = plugin.roleRegistry().require(roleId);
            sender.sendMessage(
                    "§e- §f"
                            + role.displayName()
                            + " §8("
                            + role.id()
                            + ")"
            );
            sender.sendMessage("  §7" + role.description());
        }

        plugin.roleSelectionService().selectedRole(playerId).ifPresent(roleId -> {
            RoleDefinition role = plugin.roleRegistry().require(roleId);
            sender.sendMessage("§a현재 선택: " + role.displayName());
        });

        return true;
    }

    private boolean handleRoleChoose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /space role choose <roleId>");
            return true;
        }

        RoleId roleId = new RoleId(args[2].toLowerCase());
        if (plugin.roleRegistry().find(roleId).isEmpty()) {
            sender.sendMessage("§c존재하지 않는 직업 ID입니다.");
            return true;
        }

        PlayerId playerId = PlayerId.of(player.getUniqueId());
        RoleSelectionResult result = plugin.roleSelectionService().select(playerId, roleId);

        switch (result) {
            case SELECTED -> {
                RoleDefinition role = plugin.roleRegistry().require(roleId);
                sender.sendMessage("§a직업을 확정했습니다: " + role.displayName());
            }
            case CANDIDATES_NOT_PREPARED ->
                    sender.sendMessage("§c아직 직업 후보가 생성되지 않았습니다.");
            case ROLE_NOT_OFFERED ->
                    sender.sendMessage("§c본인에게 제시된 후보가 아닌 직업은 선택할 수 없습니다.");
            case ROLE_FULL ->
                    sender.sendMessage("§c해당 직업은 이미 선택 가능 인원이 가득 찼습니다.");
            case ALREADY_SELECTED ->
                    sender.sendMessage("§c이미 직업을 확정했습니다.");
        }

        return true;
    }

    private void sendRoleStatus(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f직업 선택 상태");
        sender.sendMessage(
                "§7후보 생성 인원: §f"
                        + plugin.roleSelectionService().preparedPlayerCount()
        );
        sender.sendMessage(
                "§7선택 완료 인원: §f"
                        + plugin.roleSelectionService().selectedPlayerCount()
        );

        for (RoleDefinition role : plugin.roleRegistry().all()) {
            sender.sendMessage(
                    "§7"
                            + role.displayName()
                            + ": §f"
                            + plugin.roleSelectionService().selectedCount(role.id())
                            + "§7/§f"
                            + role.maxCopies()
            );
        }
    }

    private boolean handleLobby(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendLobbyUsage(sender);
            return true;
        }

        return switch (args[1].toLowerCase()) {
            case "join" -> handleLobbyJoin(sender);
            case "leave" -> handleLobbyLeave(sender);
            case "status" -> {
                sendLobbyStatus(sender);
                yield true;
            }
            case "start" -> handleLobbyStart(sender);
            default -> {
                sendLobbyUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleLobbyJoin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        LobbyJoinResult result = plugin.lobbyService().join(
                PlayerId.of(player.getUniqueId())
        );

        switch (result) {
            case JOINED -> sender.sendMessage("§a대기실에 참가했습니다.");
            case ALREADY_JOINED -> sender.sendMessage("§e이미 대기실에 참가 중입니다.");
            case FULL -> sender.sendMessage("§c대기실 정원이 가득 찼습니다.");
            case MATCH_ALREADY_STARTED -> sender.sendMessage("§c이미 게임이 시작되어 참가할 수 없습니다.");
        }

        sendLobbyStatus(sender);
        return true;
    }

    private boolean handleLobbyLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 게임 안의 플레이어만 사용할 수 있습니다.");
            return true;
        }

        LobbyLeaveResult result = plugin.lobbyService().leave(
                PlayerId.of(player.getUniqueId())
        );

        switch (result) {
            case LEFT -> sender.sendMessage("§a대기실에서 나왔습니다.");
            case NOT_JOINED -> sender.sendMessage("§e현재 대기실에 참가하고 있지 않습니다.");
            case MATCH_ALREADY_STARTED -> sender.sendMessage("§c게임이 시작된 뒤에는 대기실에서 나갈 수 없습니다.");
        }

        sendLobbyStatus(sender);
        return true;
    }

    private boolean handleLobbyStart(CommandSender sender) {
        if (!sender.hasPermission("spacesurvival.admin")) {
            sender.sendMessage("§c게임 시작 권한이 없습니다.");
            return true;
        }

        try {
            plugin.lobbyService().start();
            sender.sendMessage("§a게임 시작 준비 단계로 전환했습니다.");
            sendLobbyStatus(sender);
        } catch (LobbyStartException exception) {
            LobbySnapshot snapshot = plugin.lobbyService().snapshot();
            sender.sendMessage(
                    "§c게임을 시작할 수 없습니다. 참가 인원: "
                            + snapshot.playerCount()
                            + "/"
                            + snapshot.minPlayers()
            );
        }

        return true;
    }

    private void sendLobbyStatus(CommandSender sender) {
        LobbySnapshot snapshot = plugin.lobbyService().snapshot();

        sender.sendMessage("§6[우주 생존] §f대기실 상태");
        sender.sendMessage(
                "§7참가 인원: §f"
                        + snapshot.playerCount()
                        + "§7/§f"
                        + snapshot.maxPlayers()
                        + " §8(최소 "
                        + snapshot.minPlayers()
                        + "명)"
        );
        sender.sendMessage("§7접속 중: §f" + snapshot.connectedPlayers());

        if (snapshot.started()) {
            sender.sendMessage("§7게임 단계: §e" + snapshot.gamePhase());
        } else {
            sender.sendMessage("§7게임 단계: §e대기 중");
        }
    }

    private boolean handleMap(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("generate")) {
            sender.sendMessage("§c사용법: /space map generate [seed]");
            return true;
        }

        long seed;
        if (args.length >= 3) {
            try {
                seed = Long.parseLong(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage("§c시드는 정수여야 합니다.");
                sender.sendMessage("§7사용법: /space map generate [seed]");
                return true;
            }
        } else {
            seed = ThreadLocalRandom.current().nextLong();
        }

        sendGeneratedMap(sender, mapDebugService.generate(seed));
        return true;
    }

    private void sendGeneratedMap(
            CommandSender sender,
            MapDebugService.DebugMapResult result
    ) {
        sender.sendMessage("§6[우주 생존] §f논리 맵 생성 결과");
        sender.sendMessage("§7시드: §f" + result.seed());
        sender.sendMessage("§7타일: §f" + result.map().tileIds().size());
        sender.sendMessage("§7연결: §f" + result.map().connections().size());
        sender.sendMessage("§7막다른 길: §f" + result.map().deadEndCount());

        sender.sendMessage("§e[타일]");
        for (String line : result.tileLines()) {
            sender.sendMessage("§7- §f" + line);
        }

        sender.sendMessage("§e[연결]");
        for (String line : result.connectionLines()) {
            sender.sendMessage("§7- §f" + line);
        }
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("§6[우주 생존] §f개발 서버 상태");
        sender.sendMessage("§7플러그인: §a정상");
        sender.sendMessage("§7버전: §f" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7코어: §f" + BootstrapMarker.moduleName());
        sender.sendMessage(
                "§7플레이어 설정: §f"
                        + plugin.configuration().game().minPlayers()
                        + "§7~§f"
                        + plugin.configuration().game().maxPlayers()
        );
        sender.sendMessage("§7현재 개발: §ePT-001~005 Playtest Integration");
    }

    private void sendRoleUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space role prepare [seed]");
        sender.sendMessage("§c사용법: /space role candidates");
        sender.sendMessage("§c사용법: /space role gui");
        sender.sendMessage("§c사용법: /space role choose <roleId>");
        sender.sendMessage("§c사용법: /space role status");
    }

    private void sendFacilityUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space facility list");
        sender.sendMessage("§c사용법: /space facility status <id>");
        sender.sendMessage("§c사용법: /space facility set <id> <normal|damaged|offline|quarantined>");
        sender.sendMessage("§c사용법: /space facility reset");
    }

    private void sendShipUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space ship status");
        sender.sendMessage("§c사용법: /space ship set <power|oxygen|hull|reactor> <0-100>");
        sender.sendMessage("§c사용법: /space ship reset");
    }

    private void sendRuntimeUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space runtime start");
        sender.sendMessage("§c사용법: /space runtime status");
        sender.sendMessage("§c사용법: /space runtime stop");
    }

    private void sendLobbyUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space lobby join");
        sender.sendMessage("§c사용법: /space lobby leave");
        sender.sendMessage("§c사용법: /space lobby status");
        sender.sendMessage("§c사용법: /space lobby start");
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§c사용법: /space status");
        sender.sendMessage("§c사용법: /space lobby join|leave|status|start");
        sender.sendMessage("§c사용법: /space briefing");
        sender.sendMessage("§c사용법: /space runtime start|status|stop");
        sender.sendMessage("§c사용법: /space ship status|set|reset");
        sender.sendMessage("§c사용법: /space facility list|status|set|reset");
        sender.sendMessage("§c사용법: /space engineering status|adjust");
        sender.sendMessage("§c사용법: /space medical status|damage|treat|condition");
        sender.sendMessage("§c사용법: /space actions <facilityId>");
        sender.sendMessage("§c사용법: /space resource status|add|process|item|nodes|itemsadder");
        sender.sendMessage("§c사용법: /space objective prepare|status|secret|advance|catalog");
        sender.sendMessage("§c사용법: /space event list|trigger|random|status");
        sender.sendMessage("§c사용법: /space meeting start|emergency|status|end");
        sender.sendMessage("§c사용법: /space sanction vote|resolve|execute|status");
        sender.sendMessage("§c사용법: /space pvp status|emergency|scenario|special");
        sender.sendMessage("§c사용법: /space radio status|give|remove|outage|longrange");
        sender.sendMessage("§c사용법: /space voice");
        sender.sendMessage("§c사용법: /space scenario list|start|status|reset");
        sender.sendMessage("§c사용법: /space infection outbreak|status|expose|advance|test|suppress|cure");
        sender.sendMessage("§c사용법: /space death status|kill");
        sender.sendMessage("§c사용법: /space infected status");
        sender.sendMessage("§c사용법: /space pve status|spawn");
        sender.sendMessage("§c사용법: /space return status|check|navigation|prepare|hold|fail|reset");
        sender.sendMessage("§c사용법: /space result contribution|scenario|evaluate|status");
        sender.sendMessage("§c사용법: /space match start|devstart|status|reset|bridge");
        sender.sendMessage("§c사용법: /space structure status");
        sender.sendMessage("§c사용법: /space door list|set|repair");
        sender.sendMessage("§c사용법: /space director status|force");
        sender.sendMessage("§c사용법: /space role prepare|candidates|gui|choose|status");
        sender.sendMessage("§c사용법: /space map generate [seed]");
    }
}
