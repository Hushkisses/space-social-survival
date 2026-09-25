package com.hushkisses.spacesurvival.paper.social;

import com.hushkisses.spacesurvival.facility.DefaultFacilityCatalog;
import com.hushkisses.spacesurvival.facility.FacilityStatus;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.player.PlayerId;
import com.hushkisses.spacesurvival.role.DefaultRoleCatalog;
import com.hushkisses.spacesurvival.social.meeting.*;
import com.hushkisses.spacesurvival.social.sanction.*;
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

public final class MeetingGuiService implements Listener {

    private static final String CHOICE_TITLE = "§8회의 처분 선택";
    private static final String TARGET_TITLE = "§8처분 대상 선택";

    private final SpaceSurvivalPlugin plugin;
    private SanctionVoteService activeVote;
    private SanctionVoteResult lastResult;
    private final Map<UUID, SanctionType> pendingSanction = new HashMap<>();
    private final Map<UUID, Map<Integer, PlayerId>> targetSlots = new HashMap<>();

    public MeetingGuiService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public MeetingStartResult startRegular() {
        MeetingStartResult result = plugin.meetingService().startRegular(
                plugin.lobbyService().snapshot().players(),
                plugin.radioRuntimeState().longRangeAvailable()
        );
        if (result.started()) {
            begin(result.meeting(), "§6[회의] §f일반 회의가 시작되었습니다.");
        }
        return result;
    }

    public MeetingStartResult startEmergency(EmergencyMeetingReason reason) {
        MeetingStartResult result = plugin.meetingService().startEmergency(
                plugin.lobbyService().snapshot().players(),
                reason
        );
        if (result.started()) {
            begin(
                    result.meeting(),
                    "§c[긴급회의] §f사유: " + emergencyReasonName(reason)
            );
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
        return activeVote != null && plugin.meetingService().activeMeeting().isPresent();
    }

    public void resetRuntime() {
        activeVote = null;
        lastResult = null;
        pendingSanction.clear();
        targetSlots.clear();
    }

    public void vote(Player player, SanctionChoice choice) {
        if (!hasActiveVote()) {
            throw new IllegalStateException("활성 회의가 없습니다.");
        }

        activeVote.vote(PlayerId.of(player.getUniqueId()), choice);
        player.closeInventory();
        player.sendMessage("§a[회의] §f투표를 기록했습니다.");

        tryAutoResolve();
    }

    public SanctionVoteResult resolveNow() {
        if (!hasActiveVote()) {
            throw new IllegalStateException("진행 중인 처분 투표가 없습니다.");
        }

        SanctionVoteResult result = activeVote.resolve();
        lastResult = result;

        SanctionExecutionResult execution = plugin.sanctionExecutor().execute(
                result.winningChoice(),
                executionContext()
        );

        plugin.meetingService().resolveActive();
        activeVote = null;
        pendingSanction.clear();
        targetSlots.clear();

        broadcastResult(result, execution);
        return result;
    }

    public void cancel() {
        if (plugin.meetingService().activeMeeting().isPresent()) {
            plugin.meetingService().cancelActive();
        }
        activeVote = null;
        pendingSanction.clear();
        targetSlots.clear();
        plugin.getServer().broadcastMessage("§6[회의] §f회의가 종료되었습니다.");
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(CHOICE_TITLE) && !title.equals(TARGET_TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!hasActiveVote()) {
            player.closeInventory();
            player.sendMessage("§c활성 회의가 없습니다.");
            return;
        }

        if (title.equals(CHOICE_TITLE)) {
            SanctionType type = sanctionAt(event.getRawSlot());
            if (type == null) return;

            if (type == SanctionType.NO_ACTION) {
                cast(player, SanctionChoice.noAction());
                return;
            }

            pendingSanction.put(player.getUniqueId(), type);
            openTargets(player);
            return;
        }

        Map<Integer, PlayerId> slots = targetSlots.get(player.getUniqueId());
        if (slots == null) return;

        PlayerId target = slots.get(event.getRawSlot());
        SanctionType type = pendingSanction.get(player.getUniqueId());
        if (target == null || type == null) return;

        cast(player, new SanctionChoice(type, target));
    }

    private void begin(MeetingSession meeting, String broadcast) {
        activeVote = new SanctionVoteService(meeting);
        lastResult = null;
        pendingSanction.clear();
        targetSlots.clear();

        plugin.getServer().broadcastMessage(broadcast);
        plugin.telemetryService().increment("meeting.started");

        for (PlayerId participant : meeting.participants()) {
            Player player = plugin.getServer().getPlayer(participant.value());
            if (player != null) {
                openChoices(player);
            }
        }
    }

    private void openChoices(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, CHOICE_TITLE);

        inventory.setItem(10, item(Material.LIME_DYE, "§a무조치", "현재 처분을 하지 않습니다."));
        inventory.setItem(11, item(Material.POTION, "§b의료 검사", "대상에게 의료 검사를 명령합니다."));
        inventory.setItem(12, item(Material.IRON_SWORD, "§e무장해제", "대상의 무장을 제한합니다."));
        inventory.setItem(14, item(Material.IRON_BARS, "§6감금", "대상을 구금합니다."));
        inventory.setItem(15, item(Material.IRON_DOOR, "§d출입 제한", "시설 접근을 제한합니다."));
        inventory.setItem(16, item(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "§c추방", "대상을 우주선에서 추방합니다."));

        player.openInventory(inventory);
    }

    private void openTargets(Player player) {
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElseThrow();
        int size = meeting.participants().size() <= 9 ? 18 : 27;
        Inventory inventory = Bukkit.createInventory(null, size, TARGET_TITLE);

        LinkedHashMap<Integer, PlayerId> slots = new LinkedHashMap<>();
        int slot = 9;
        for (PlayerId participant : meeting.participants()) {
            String name = Optional.ofNullable(plugin.getServer().getPlayer(participant.value()))
                    .map(Player::getName)
                    .orElse(participant.value().toString().substring(0, 8));

            inventory.setItem(
                    slot,
                    item(Material.PAPER, "§f" + name, "§7이 플레이어를 처분 대상으로 선택합니다.")
            );
            slots.put(slot, participant);
            slot++;
        }

        targetSlots.put(player.getUniqueId(), Map.copyOf(slots));
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
        MeetingSession meeting = plugin.meetingService().activeMeeting().orElse(null);
        if (meeting == null || activeVote == null) return;

        long onlineParticipants = meeting.participants().stream()
                .map(PlayerId::value)
                .map(plugin.getServer()::getPlayer)
                .filter(Objects::nonNull)
                .count();

        if (onlineParticipants > 0 && activeVote.votes().size() >= onlineParticipants) {
            resolveNow();
        }
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

        if (!execution.executed()) {
            plugin.getServer().broadcastMessage(
                    "§c[집행 실패] §f" + execution.failureReason()
            );
        }

        plugin.telemetryService().increment("meeting.resolved");
        plugin.telemetryService().increment(
                "sanction." + choice.sanction().name().toLowerCase(Locale.ROOT)
        );
    }

    private String playerName(PlayerId id) {
        Player player = plugin.getServer().getPlayer(id.value());
        return player == null
                ? id.value().toString().substring(0, 8)
                : player.getName();
    }

    private static SanctionType sanctionAt(int slot) {
        return switch (slot) {
            case 10 -> SanctionType.NO_ACTION;
            case 11 -> SanctionType.MEDICAL_CHECK;
            case 12 -> SanctionType.DISARM;
            case 14 -> SanctionType.DETAIN;
            case 15 -> SanctionType.ACCESS_RESTRICT;
            case 16 -> SanctionType.EJECT;
            default -> null;
        };
    }

    private static ItemStack item(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("§7" + lore));
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

    private static String emergencyReasonName(EmergencyMeetingReason reason) {
        return switch (reason) {
            case BODY_FOUND -> "시체 발견";
            case INFECTION_ALERT -> "감염 경보";
            case REACTOR_CRITICAL -> "원자로 중대사고";
            case SECURITY_ALERT -> "보안 경보";
            case SPECIAL_EVENT -> "특수 사건";
        };
    }
}
