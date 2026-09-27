package com.hushkisses.spacesurvival.paper.social;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.social.meeting.*;
import com.hushkisses.spacesurvival.social.sanction.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class MeetingGuiService implements Listener {

    private static final String CHOICE_TITLE = "§8회의 — 처분 선택";
    private static final String TARGET_TITLE = "§8회의 — 대상 선택";
    private static final String CONFIRM_TITLE = "§8회의 — 처분 확인";
    private static final int RESULT_DISPLAY_SECONDS = 5;

    private final SpaceSurvivalPlugin plugin;
    private SanctionVoteService activeVote;
    private SanctionVoteResult lastResult;
    private MeetingUxPhase phase = MeetingUxPhase.IDLE;
    private int secondsRemaining;
    private int totalPhaseSeconds;
    private BukkitTask phaseTask;
    private BossBar phaseBar;

    private final Map<UUID, PlayerId> pendingTarget = new HashMap<>();
    private final Map<UUID, SanctionChoice> pendingConfirmation = new HashMap<>();
    private final Map<UUID, Map<Integer, PlayerId>> targetSlots = new HashMap<>();
    private final Set<UUID> transitioningInventory = new HashSet<>();

    public MeetingGuiService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public MeetingStartResult startRegular() {
        MeetingStartResult result = plugin.meetingService().startRegular(
                plugin.lobbyService().snapshot().players(),
                plugin.radioRuntimeState().longRangeAvailable()
        );
        if (result.started()) {
            begin(result.meeting());
        }
        return result;
    }

    public MeetingStartResult startEmergency(EmergencyMeetingReason reason) {
        MeetingStartResult result = plugin.meetingService().startEmergency(
                plugin.lobbyService().snapshot().players(),
                reason
        );
        if (result.started()) {
            begin(result.meeting());
        }
        return result;
    }

    public int voteCount() {
        return activeVote == null ? 0 : activeVote.votes().size();
    }

    public Optional<SanctionVoteResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    public boolean hasActiveVote() {
        return activeVote != null && meetingActive();
    }

    public boolean meetingActive() {
        return (phase == MeetingUxPhase.DISCUSSION || phase == MeetingUxPhase.VOTING)
                && plugin.meetingService().activeMeeting().isPresent();
    }

    public void resetRuntime() {
        cancelPhaseTask();
        hidePhaseBar();
        activeVote = null;
        lastResult = null;
        phase = MeetingUxPhase.IDLE;
        secondsRemaining = 0;
        totalPhaseSeconds = 0;
        pendingTarget.clear();
        pendingConfirmation.clear();
        targetSlots.clear();
        transitioningInventory.clear();
    }

    public void stop() {
        resetRuntime();
    }

    public void vote(Player player, SanctionChoice choice) {
        if (!hasActiveVote()) {
            throw new IllegalStateException("활성 회의가 없습니다.");
        }
        if (phase != MeetingUxPhase.VOTING) {
            throw new IllegalStateException("아직 투표 단계가 아닙니다.");
        }

        activeVote.vote(PlayerId.of(player.getUniqueId()), choice);
        pendingTarget.remove(player.getUniqueId());
        pendingConfirmation.remove(player.getUniqueId());
        targetSlots.remove(player.getUniqueId());

        player.closeInventory();
        player.sendMessage("§a[회의] §f투표가 기록되었습니다. 결과가 나올 때까지 기다리십시오.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);

        broadcastVoteProgress();
        renderPhaseBar();
        tryAutoResolve();
    }

    public SanctionVoteResult resolveNow() {
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);
        if (activeVote == null || meeting == null) {
            throw new IllegalStateException("진행 중인 처분 투표가 없습니다.");
        }

        cancelPhaseTask();
        phase = MeetingUxPhase.RESULT;

        SanctionVoteResult result = activeVote.resolve();
        lastResult = result;

        SanctionExecutionResult execution = plugin.sanctionExecutor().execute(
                result.winningChoice(),
                executionContext()
        );
        plugin.physicalSanctionService().apply(result.winningChoice(), execution);

        plugin.meetingService().resolveActive();
        activeVote = null;
        pendingTarget.clear();
        pendingConfirmation.clear();
        targetSlots.clear();

        closeMeetingInventories(meeting);
        broadcastResult(result, execution);
        showResultPresentation(meeting, result, execution);
        return result;
    }

    public void cancel() {
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);
        if (meeting != null) {
            plugin.meetingService().cancelActive();
            closeMeetingInventories(meeting);
        }

        cancelPhaseTask();
        hidePhaseBar();
        activeVote = null;
        phase = MeetingUxPhase.IDLE;
        secondsRemaining = 0;
        totalPhaseSeconds = 0;
        pendingTarget.clear();
        pendingConfirmation.clear();
        targetSlots.clear();
        plugin.getServer().broadcastMessage("§6[회의] §f회의가 취소되었습니다. 일반 임무로 복귀합니다.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!isMeetingInventory(title)) {
            return;
        }

        event.setCancelled(true);

        if (!hasActiveVote() || phase != MeetingUxPhase.VOTING) {
            player.closeInventory();
            player.sendMessage("§c현재 투표할 수 있는 단계가 아닙니다.");
            return;
        }

        if (activeVote.votes().containsKey(PlayerId.of(player.getUniqueId()))) {
            player.closeInventory();
            player.sendMessage("§7이미 투표를 완료했습니다.");
            return;
        }

        if (title.equals(TARGET_TITLE)) {
            if (event.getRawSlot() == 4) {
                cast(player, SanctionChoice.noAction());
                return;
            }

            Map<Integer, PlayerId> slots = targetSlots.get(player.getUniqueId());
            if (slots == null) return;

            PlayerId target = slots.get(event.getRawSlot());
            if (target == null) return;

            pendingTarget.put(player.getUniqueId(), target);
            transition(player, () -> openChoices(player));
            return;
        }

        if (title.equals(CHOICE_TITLE)) {
            SanctionType type = sanctionAt(event.getRawSlot());
            PlayerId target = pendingTarget.get(player.getUniqueId());
            if (type == null || target == null) return;

            SanctionChoice choice = new SanctionChoice(type, target);
            if (requiresConfirmation(type)) {
                pendingConfirmation.put(player.getUniqueId(), choice);
                transition(player, () -> openConfirmation(player, choice));
            } else {
                cast(player, choice);
            }
            return;
        }

        SanctionChoice choice = pendingConfirmation.get(player.getUniqueId());
        if (choice == null) return;

        if (event.getRawSlot() == 11) {
            cast(player, choice);
        } else if (event.getRawSlot() == 15) {
            pendingConfirmation.remove(player.getUniqueId());
            transition(player, () -> openChoices(player));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isMeetingInventory(event.getView().getTitle())) return;
        if (phase != MeetingUxPhase.VOTING || !hasActiveVote()) return;

        UUID uuid = player.getUniqueId();
        if (transitioningInventory.contains(uuid)) {
            return;
        }

        PlayerId playerId = PlayerId.of(uuid);
        if (activeVote.votes().containsKey(playerId)) {
            return;
        }

        pendingTarget.remove(uuid);
        pendingConfirmation.remove(uuid);
        targetSlots.remove(uuid);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()
                    || phase != MeetingUxPhase.VOTING
                    || !hasActiveVote()
                    || activeVote.votes().containsKey(playerId)) {
                return;
            }
            player.sendMessage("§e[회의] §f투표가 아직 끝나지 않아 투표 창을 다시 엽니다.");
            openTargets(player);
        }, 20L);
    }

    private void begin(MeetingSession meeting) {
        cancelPhaseTask();
        hidePhaseBar();

        activeVote = new SanctionVoteService(meeting);
        lastResult = null;
        pendingTarget.clear();
        pendingConfirmation.clear();
        targetSlots.clear();
        transitioningInventory.clear();

        phase = MeetingUxPhase.DISCUSSION;
        totalPhaseSeconds = plugin.configuration().meetingUx().discussionSeconds();
        secondsRemaining = totalPhaseSeconds;

        phaseBar = Bukkit.createBossBar(
                "",
                meeting.type() == MeetingType.EMERGENCY ? BarColor.RED : BarColor.YELLOW,
                BarStyle.SOLID
        );
        phaseBar.setVisible(true);
        syncBossBarPlayers(meeting);
        renderPhaseBar();

        String heading = meeting.type() == MeetingType.EMERGENCY
                ? "§c§l긴급 회의"
                : "§6§l승무원 회의";
        String reason = meetingReason(meeting);

        plugin.getServer().broadcastMessage("§6[회의] §f" + reason);
        plugin.getServer().broadcastMessage(
                "§7참가자: §f" + String.join(" §8· §f", participantNames(meeting))
        );
        plugin.getServer().broadcastMessage(
                "§e토론 " + secondsRemaining + "초 §7후 투표가 시작됩니다."
        );

        forEachOnlineParticipant(meeting, player -> {
            player.closeInventory();
            player.sendTitle(
                    heading,
                    "§f" + reason + " §7— §e토론 " + secondsRemaining + "초",
                    5,
                    50,
                    10
            );
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1.0f, 0.8f);
        });

        plugin.telemetryService().increment("meeting.started");
        plugin.telemetryService().event("meeting.phase", "discussion");

        phaseTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tickPhase,
                20L,
                20L
        );
    }

    private void tickPhase() {
        if (!meetingActive()) {
            cancelPhaseTask();
            hidePhaseBar();
            return;
        }

        secondsRemaining = Math.max(0, secondsRemaining - 1);

        if (phase == MeetingUxPhase.DISCUSSION && secondsRemaining <= 0) {
            beginVoting();
            return;
        }

        if (phase == MeetingUxPhase.VOTING && secondsRemaining <= 0) {
            resolveNow();
            return;
        }

        renderPhaseBar();
    }

    private void beginVoting() {
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);
        if (meeting == null || activeVote == null) {
            cancel();
            return;
        }

        phase = MeetingUxPhase.VOTING;
        totalPhaseSeconds = plugin.configuration().meetingUx().votingSeconds();
        secondsRemaining = totalPhaseSeconds;

        plugin.getServer().broadcastMessage(
                "§d[회의] §f투표를 시작합니다. §7개별 선택 내용은 공개되지 않습니다."
        );

        forEachOnlineParticipant(meeting, player -> {
            player.sendTitle(
                    "§d§l투표 시작",
                    "§f먼저 처분할 승무원을 선택하십시오 · §e" + secondsRemaining + "초",
                    5,
                    35,
                    10
            );
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.4f);
            openTargets(player);
        });

        plugin.telemetryService().event("meeting.phase", "voting");
        renderPhaseBar();
    }

    private void openChoices(Player player) {
        if (phase != MeetingUxPhase.VOTING || activeVote == null) return;

        PlayerId target = pendingTarget.get(player.getUniqueId());
        if (target == null) {
            openTargets(player);
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 36, CHOICE_TITLE);
        String targetName = playerName(target);

        inventory.setItem(4, item(
                Material.PLAYER_HEAD,
                "§f대상: §e" + targetName,
                List.of(
                        "§7이 승무원에게 적용할 처분을 선택하십시오.",
                        "§8뒤로 가려면 창을 닫으십시오."
                )
        ));

        inventory.setItem(10, item(
                Material.POTION,
                "§b의료 검사",
                List.of(
                        "§7" + targetName + "에게 의료 검사를 명령합니다.",
                        "§8집행 조건: 의료실 사용 가능"
                )
        ));
        inventory.setItem(11, item(
                Material.IRON_SWORD,
                "§e무장해제",
                List.of(
                        "§7" + targetName + "의 무장을 제한합니다.",
                        "§8집행 조건: 보안 담당 권한"
                )
        ));
        inventory.setItem(13, item(
                Material.IRON_BARS,
                "§6감금",
                List.of(
                        "§7" + targetName + "을(를) 구금합니다.",
                        "§8집행 조건: 보안 담당 권한 + 감금 가능",
                        "§e최종 확인 필요"
                )
        ));
        inventory.setItem(14, item(
                Material.IRON_DOOR,
                "§d출입 제한",
                List.of(
                        "§7" + targetName + "의 시설 접근을 제한합니다.",
                        "§8집행 조건: 보안 담당 권한",
                        "§e최종 확인 필요"
                )
        ));
        inventory.setItem(16, item(
                Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
                "§c추방",
                List.of(
                        "§7" + targetName + "을(를) 우주선에서 추방합니다.",
                        "§8집행 조건: 사용 가능한 에어록",
                        "§c최종 확인 필요"
                )
        ));

        inventory.setItem(31, item(
                Material.ARROW,
                "§7대상 다시 선택",
                List.of("§7창을 닫으면 대상 선택 화면으로 돌아갑니다.")
        ));

        player.openInventory(inventory);
    }

    private void openTargets(Player player) {
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElseThrow();
        int size = meeting.participants().size() <= 9 ? 27 : 36;
        Inventory inventory = Bukkit.createInventory(null, size, TARGET_TITLE);

        inventory.setItem(4, item(
                Material.LIME_DYE,
                "§a아무도 처분하지 않음",
                List.of(
                        "§7이번 회의에서는 처분하지 않습니다.",
                        "§a클릭하면 무조치로 즉시 투표합니다."
                )
        ));

        LinkedHashMap<Integer, PlayerId> slots = new LinkedHashMap<>();
        int slot = 9;
        for (PlayerId participant : meeting.participants()) {
            Player online = plugin.getServer().getPlayer(participant.value());
            String name = online == null
                    ? participant.value().toString().substring(0, 8)
                    : online.getName();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta rawMeta = head.getItemMeta();
            if (rawMeta instanceof SkullMeta skullMeta && online != null) {
                skullMeta.setOwningPlayer(online);
                rawMeta = skullMeta;
            }
            rawMeta.setDisplayName("§f" + name);
            rawMeta.setLore(List.of(
                    "§7먼저 이 승무원을 선택합니다.",
                    "§7다음 화면에서 처분을 결정합니다."
            ));
            head.setItemMeta(rawMeta);

            inventory.setItem(slot, head);
            slots.put(slot, participant);
            slot++;
        }

        inventory.setItem(
                size - 5,
                item(
                        Material.CLOCK,
                        "§d투표 진행",
                        List.of(
                                "§7남은 시간: §f" + secondsRemaining + "초",
                                "§7투표 완료: §f" + voteCount() + "/" + eligibleVoterCount(),
                                "§8개별 투표 내용은 공개되지 않습니다."
                        )
                )
        );

        targetSlots.put(player.getUniqueId(), Map.copyOf(slots));
        player.openInventory(inventory);
    }

    private void openConfirmation(Player player, SanctionChoice choice) {
        Inventory inventory = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        String target = playerName(choice.target());

        inventory.setItem(13, item(
                Material.PAPER,
                "§f" + sanctionName(choice.sanction()) + " → " + target,
                List.of(
                        "§7이 선택은 회의 결과에 반영됩니다.",
                        "§7투표가 승리해도 기존 집행 조건을 충족해야 실제 집행됩니다."
                )
        ));
        inventory.setItem(11, item(
                Material.LIME_CONCRETE,
                "§a이 선택으로 투표",
                List.of("§a클릭하여 최종 확정")
        ));
        inventory.setItem(15, item(
                Material.RED_CONCRETE,
                "§c처분 다시 선택",
                List.of("§7같은 대상에 대한 처분 선택으로 돌아갑니다.")
        ));

        player.openInventory(inventory);
    }

    private void cast(Player player, SanctionChoice choice) {
        try {
            vote(player, choice);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            player.sendMessage("§c투표할 수 없습니다: " + exception.getMessage());
        }
    }

    private void tryAutoResolve() {
        if (phase != MeetingUxPhase.VOTING || activeVote == null) return;

        int eligible = eligibleVoterCount();
        if (eligible > 0 && activeVote.votes().size() >= eligible) {
            resolveNow();
        }
    }

    private int eligibleVoterCount() {
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);
        if (meeting == null) return 0;

        int online = (int) meeting.participants().stream()
                .map(PlayerId::value)
                .map(plugin.getServer()::getPlayer)
                .filter(Objects::nonNull)
                .count();

        return online > 0 ? online : meeting.participants().size();
    }

    private void broadcastVoteProgress() {
        int eligible = eligibleVoterCount();
        plugin.getServer().broadcastMessage(
                "§d[회의] §f투표 진행 §e" + voteCount() + "/" + eligible
                        + " §7— 선택 내용은 비공개"
        );
    }

    private SanctionExecutionContext executionContext() {
        boolean medicalAvailable = switch (
                plugin.facilityRegistry().require(DefaultFacilityCatalog.MEDICAL).status()
        ) {
            case NORMAL, DAMAGED -> true;
            case OFFLINE, QUARANTINED -> false;
        };

        boolean securityAuthorized = plugin.lobbyService().snapshot().players().stream()
                .filter(playerId -> plugin.lobbyService().playerState(playerId)
                        .map(state -> state.isAlive())
                        .orElse(false))
                .anyMatch(playerId -> plugin.roleSelectionService()
                        .selectedRole(playerId)
                        .map(DefaultRoleCatalog.SECURITY::equals)
                        .orElse(false));

        boolean airlockAvailable = plugin.shipWorldService().activeSnapshot()
                .map(snapshot -> snapshot.generatedMap().tileIds().stream()
                        .anyMatch(tileId -> tileId.value().startsWith("airlock")))
                .orElse(false);

        return new SanctionExecutionContext(
                medicalAvailable,
                securityAuthorized,
                true,
                airlockAvailable
        );
    }

    private void broadcastResult(
            SanctionVoteResult result,
            SanctionExecutionResult execution
    ) {
        SanctionChoice choice = result.winningChoice();
        String target = choice.target() == null
                ? ""
                : " → " + playerName(choice.target());

        plugin.getServer().broadcastMessage(
                "§6[회의 결과] §f"
                        + sanctionName(choice.sanction())
                        + target
                        + (result.tied() ? " §e(동률 → 무조치)" : "")
        );

        if (execution.executed()) {
            plugin.getServer().broadcastMessage("§a[집행 결과] §f처분이 정상적으로 집행되었습니다.");
        } else {
            plugin.getServer().broadcastMessage(
                    "§c[집행 실패] §f" + executionFailureName(execution.failureReason())
            );
        }

        plugin.telemetryService().increment("meeting.resolved");
        plugin.telemetryService().increment(
                "sanction." + choice.sanction().name().toLowerCase(Locale.ROOT)
        );
        plugin.telemetryService().event(
                "meeting.result",
                choice.sanction().name()
                        + ":"
                        + (execution.executed() ? "executed" : "failed")
        );
    }

    private void showResultPresentation(
            MeetingSession meeting,
            SanctionVoteResult result,
            SanctionExecutionResult execution
    ) {
        SanctionChoice choice = result.winningChoice();
        String target = choice.target() == null ? "" : " → " + playerName(choice.target());
        String resultText = sanctionName(choice.sanction()) + target;
        String subtitle = execution.executed()
                ? "§a집행 완료"
                : "§c집행 실패 · " + executionFailureName(execution.failureReason());

        if (phaseBar == null) {
            phaseBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        }
        phaseBar.setColor(execution.executed() ? BarColor.GREEN : BarColor.RED);
        phaseBar.setProgress(1.0);
        phaseBar.setTitle("§6회의 결과 §7— §f" + resultText);
        syncBossBarPlayers(meeting);
        phaseBar.setVisible(true);

        forEachOnlineParticipant(meeting, player -> {
            player.sendTitle(
                    "§6§l회의 결과",
                    "§f" + resultText + " §7— " + subtitle,
                    5,
                    70,
                    15
            );
            player.playSound(
                    player.getLocation(),
                    execution.executed()
                            ? Sound.ENTITY_PLAYER_LEVELUP
                            : Sound.ENTITY_VILLAGER_NO,
                    0.9f,
                    1.0f
            );
        });

        phaseTask = plugin.getServer().getScheduler().runTaskLater(
                plugin,
                this::finishResultPresentation,
                RESULT_DISPLAY_SECONDS * 20L
        );
    }

    private void finishResultPresentation() {
        hidePhaseBar();
        phase = MeetingUxPhase.IDLE;
        secondsRemaining = 0;
        totalPhaseSeconds = 0;
        phaseTask = null;
        plugin.getServer().broadcastMessage("§7[회의] 일반 임무로 복귀합니다.");
    }

    private void renderPhaseBar() {
        if (phaseBar == null) return;
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);
        if (meeting == null) return;

        syncBossBarPlayers(meeting);

        if (phase == MeetingUxPhase.DISCUSSION) {
            phaseBar.setColor(meeting.type() == MeetingType.EMERGENCY ? BarColor.RED : BarColor.YELLOW);
            phaseBar.setTitle(
                    "§6토론 단계 §7— §f"
                            + meetingReason(meeting)
                            + " §7· §e"
                            + secondsRemaining
                            + "초"
            );
            phaseBar.setProgress(timeProgress());
        } else if (phase == MeetingUxPhase.VOTING) {
            phaseBar.setColor(BarColor.PURPLE);
            phaseBar.setTitle(
                    "§d투표 단계 §7— §f"
                            + voteCount()
                            + "/"
                            + eligibleVoterCount()
                            + " 완료 §7· §e"
                            + secondsRemaining
                            + "초"
            );
            phaseBar.setProgress(timeProgress());
        }
    }

    private double timeProgress() {
        if (totalPhaseSeconds <= 0) return 0.0;
        return Math.max(
                0.0,
                Math.min(1.0, secondsRemaining / (double) totalPhaseSeconds)
        );
    }

    private void syncBossBarPlayers(MeetingSession meeting) {
        if (phaseBar == null) return;

        Set<UUID> participantIds = meeting.participants().stream()
                .map(PlayerId::value)
                .collect(java.util.stream.Collectors.toSet());

        for (Player shown : List.copyOf(phaseBar.getPlayers())) {
            if (!participantIds.contains(shown.getUniqueId())) {
                phaseBar.removePlayer(shown);
            }
        }

        forEachOnlineParticipant(meeting, player -> {
            if (!phaseBar.getPlayers().contains(player)) {
                phaseBar.addPlayer(player);
            }
        });
    }

    private void closeMeetingInventories(MeetingSession meeting) {
        forEachOnlineParticipant(meeting, player -> {
            if (isMeetingInventory(player.getOpenInventory().getTitle())) {
                player.closeInventory();
            }
        });
    }

    private void transition(Player player, Runnable opener) {
        UUID uuid = player.getUniqueId();
        transitioningInventory.add(uuid);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()
                    || phase != MeetingUxPhase.VOTING
                    || !hasActiveVote()) {
                transitioningInventory.remove(uuid);
                return;
            }

            opener.run();
            plugin.getServer().getScheduler().runTask(
                    plugin,
                    () -> transitioningInventory.remove(uuid)
            );
        });
    }

    private void cancelPhaseTask() {
        if (phaseTask != null) {
            phaseTask.cancel();
            phaseTask = null;
        }
    }

    private void hidePhaseBar() {
        if (phaseBar != null) {
            phaseBar.removeAll();
            phaseBar.setVisible(false);
            phaseBar = null;
        }
    }

    private List<String> participantNames(MeetingSession meeting) {
        return meeting.participants().stream()
                .map(this::playerName)
                .toList();
    }

    private void forEachOnlineParticipant(
            MeetingSession meeting,
            java.util.function.Consumer<Player> action
    ) {
        for (PlayerId participant : meeting.participants()) {
            Player player = plugin.getServer().getPlayer(participant.value());
            if (player != null) {
                action.accept(player);
            }
        }
    }

    private String playerName(PlayerId id) {
        Player player = plugin.getServer().getPlayer(id.value());
        return player == null
                ? id.value().toString().substring(0, 8)
                : player.getName();
    }

    private static boolean requiresConfirmation(SanctionType type) {
        return type == SanctionType.DETAIN
                || type == SanctionType.ACCESS_RESTRICT
                || type == SanctionType.EJECT;
    }

    private static boolean isMeetingInventory(String title) {
        return title.equals(CHOICE_TITLE)
                || title.equals(TARGET_TITLE)
                || title.equals(CONFIRM_TITLE);
    }

    private static SanctionType sanctionAt(int slot) {
        return switch (slot) {
            case 10 -> SanctionType.MEDICAL_CHECK;
            case 11 -> SanctionType.DISARM;
            case 13 -> SanctionType.DETAIN;
            case 14 -> SanctionType.ACCESS_RESTRICT;
            case 16 -> SanctionType.EJECT;
            default -> null;
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

    private static String sanctionName(SanctionType type) {
        return switch (type) {
            case NO_ACTION -> "무조치";
            case MEDICAL_CHECK -> "의료 검사";
            case DISARM -> "무장해제";
            case DETAIN -> "감금";
            case ACCESS_RESTRICT -> "출입 제한";
            case EJECT -> "추방";
        };
    }

    private static String meetingReason(MeetingSession meeting) {
        if (meeting.type() == MeetingType.REGULAR) {
            return "승무원 요청에 따른 일반 회의";
        }
        return "긴급회의 · " + emergencyReasonName(meeting.emergencyReason().orElseThrow());
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

    private static String executionFailureName(
            SanctionExecutionResult.FailureReason reason
    ) {
        if (reason == null) return "알 수 없는 집행 오류";
        return switch (reason) {
            case MEDICAL_UNAVAILABLE -> "의료실을 사용할 수 없습니다.";
            case SECURITY_AUTHORITY_REQUIRED -> "생존한 보안 담당 승무원의 권한이 필요합니다.";
            case DETENTION_UNAVAILABLE -> "감금 구역을 사용할 수 없습니다.";
            case AIRLOCK_UNAVAILABLE -> "사용 가능한 에어록이 없습니다.";
            case INVALID_TARGET -> "유효한 처분 대상이 아닙니다.";
        };
    }

    private enum MeetingUxPhase {
        IDLE,
        DISCUSSION,
        VOTING,
        RESULT
    }
}
