package com.hushkisses.spacesurvival.paper.lobby;

import com.hushkisses.spacesurvival.lobby.LobbyJoinResult;
import com.hushkisses.spacesurvival.lobby.LobbySnapshot;
import com.hushkisses.spacesurvival.paper.SpaceSurvivalPlugin;
import com.hushkisses.spacesurvival.paper.map.physical.PaperShipWorldService;
import com.hushkisses.spacesurvival.paper.match.MatchLifecycleStatus;
import com.hushkisses.spacesurvival.player.PlayerId;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class LobbyReadyService implements Listener {

    private static final int FLOOR_Y = 80;
    private static final int MIN_X = -88;
    private static final int MAX_X = -72;
    private static final int MIN_Z = -6;
    private static final int MAX_Z = 6;

    private static final int READY_MIN_X = -78;
    private static final int READY_MAX_X = -74;
    private static final int READY_MIN_Z = -2;
    private static final int READY_MAX_Z = 2;

    private static final int COUNTDOWN_SECONDS = 10;

    private final SpaceSurvivalPlugin plugin;

    private World world;
    private Location lobbySpawn;
    private BossBar readyBar;
    private BukkitTask task;
    private int countdownRemaining = -1;

    public LobbyReadyService(SpaceSurvivalPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void start() {
        if (task != null) {
            return;
        }

        world = resolveWorld();
        renderLobby(world);
        lobbySpawn = new Location(world, -84.5, FLOOR_Y + 1.0, 0.5, -90.0f, 0.0f);

        readyBar = Bukkit.createBossBar(
                "§a준비 구역으로 이동하십시오",
                BarColor.GREEN,
                BarStyle.SOLID
        );
        readyBar.setVisible(false);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            enrollForWaiting(player);
        }

        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tick,
                20L,
                20L
        );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        resetCountdown(false);

        if (readyBar != null) {
            readyBar.removeAll();
            readyBar.setVisible(false);
            readyBar = null;
        }
    }

    public void resetOnlinePlayersToLobby() {
        if (world == null) {
            world = resolveWorld();
            renderLobby(world);
            lobbySpawn = new Location(world, -84.5, FLOOR_Y + 1.0, 0.5, -90.0f, 0.0f);
        }

        resetCountdown(false);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            PlayerId playerId = PlayerId.of(player.getUniqueId());
            if (plugin.lobbyService().contains(playerId)) {
                prepareWaitingPlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> enrollForWaiting(event.getPlayer())
        );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerId playerId = PlayerId.of(event.getPlayer().getUniqueId());

        if (plugin.lobbyService().gameSession().isEmpty()) {
            plugin.lobbyService().leave(playerId);
        } else {
            plugin.lobbyService().disconnect(playerId);
        }
    }

    private void enrollForWaiting(Player player) {
        PlayerId playerId = PlayerId.of(player.getUniqueId());

        if (plugin.lobbyService().gameSession().isPresent()) {
            plugin.lobbyService().reconnect(playerId);
            return;
        }

        LobbyJoinResult result = plugin.lobbyService().join(playerId);
        if (result == LobbyJoinResult.FULL) {
            player.sendMessage("§c[우주 생존] 대기실 정원이 가득 찼습니다.");
            return;
        }
        if (result == LobbyJoinResult.MATCH_ALREADY_STARTED) {
            return;
        }

        prepareWaitingPlayer(player);

        LobbySnapshot snapshot = plugin.lobbyService().snapshot();
        player.sendMessage("§6[우주 생존] §f대기실에 입장했습니다.");
        player.sendMessage(
                "§7초록색 준비 구역으로 이동하십시오. "
                        + snapshot.playerCount()
                        + "/"
                        + snapshot.minPlayers()
                        + "명 이상이고 모두 준비되면 자동 시작합니다."
        );
    }

    private void prepareWaitingPlayer(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.teleportAsync(lobbySpawn);
    }

    private void tick() {
        if (plugin.matchOrchestrator().status() != MatchLifecycleStatus.IDLE
                || plugin.lobbyService().gameSession().isPresent()) {
            resetCountdown(false);
            return;
        }

        LobbySnapshot snapshot = plugin.lobbyService().snapshot();
        List<Player> players = onlineLobbyPlayers(snapshot);

        if (players.isEmpty()) {
            resetCountdown(false);
            return;
        }

        int ready = 0;
        for (Player player : players) {
            if (isInsideReadyZone(player.getLocation())) {
                ready++;
            }
        }

        boolean minimumMet = snapshot.playerCount() >= snapshot.minPlayers();
        boolean everyoneOnline = players.size() == snapshot.playerCount();
        boolean everyoneReady = minimumMet
                && everyoneOnline
                && ready == snapshot.playerCount();

        if (!everyoneReady) {
            boolean cancelled = countdownRemaining >= 0;
            resetCountdown(cancelled);

            for (Player player : players) {
                if (!minimumMet) {
                    player.sendActionBar(Component.text(
                            "대기실 · 현재 "
                                    + snapshot.playerCount()
                                    + "/"
                                    + snapshot.minPlayers()
                                    + "명 · 최소 인원을 기다리는 중"
                    ));
                } else {
                    player.sendActionBar(Component.text(
                            "준비 "
                                    + ready
                                    + "/"
                                    + snapshot.playerCount()
                                    + " · 모두 초록색 구역에 들어가면 자동 시작"
                    ));
                }
            }
            return;
        }

        if (countdownRemaining < 0) {
            countdownRemaining = COUNTDOWN_SECONDS;
            plugin.getServer().broadcastMessage(
                    "§a[우주 생존] §f모든 승무원이 준비되었습니다. "
                            + COUNTDOWN_SECONDS
                            + "초 후 자동으로 시작합니다."
            );
        }

        syncReadyBar(players);
        readyBar.setVisible(true);
        readyBar.setTitle("§a모두 준비 완료 §7— §f" + countdownRemaining + "초 후 시작");
        readyBar.setProgress(Math.max(
                0.0,
                Math.min(1.0, countdownRemaining / (double) COUNTDOWN_SECONDS)
        ));

        for (Player player : players) {
            player.sendActionBar(Component.text(
                    "모두 준비 완료 · " + countdownRemaining + "초 후 게임 시작"
            ));
        }

        if (countdownRemaining <= 0) {
            beginMatch();
            return;
        }

        if (countdownRemaining <= 5 || countdownRemaining == COUNTDOWN_SECONDS) {
            for (Player player : players) {
                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_PLING,
                        0.8f,
                        countdownRemaining <= 3 ? 1.4f : 1.0f
                );
            }
        }

        countdownRemaining--;
    }

    private void beginMatch() {
        List<Player> players = onlineLobbyPlayers(plugin.lobbyService().snapshot());
        resetCountdown(false);

        for (Player player : players) {
            player.sendTitle(
                    "§6임무 시작",
                    "§f직업을 선택하십시오",
                    5,
                    40,
                    10
            );
        }

        long seed = ThreadLocalRandom.current().nextLong();

        try {
            plugin.matchOrchestrator().prepare(seed, false);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe(
                    "Automatic lobby start failed: "
                            + exception.getClass().getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
            plugin.getServer().broadcastMessage(
                    "§c[우주 생존] 자동 시작에 실패했습니다. 관리자에게 알려주십시오."
            );
        }
    }

    private void resetCountdown(boolean announceCancellation) {
        if (announceCancellation) {
            plugin.getServer().broadcastMessage(
                    "§e[우주 생존] §f준비 인원이 구역을 벗어나 시작 카운트다운이 취소되었습니다."
            );
        }

        countdownRemaining = -1;

        if (readyBar != null) {
            readyBar.removeAll();
            readyBar.setVisible(false);
        }
    }

    private void syncReadyBar(List<Player> players) {
        if (readyBar == null) {
            return;
        }

        for (Player shown : new ArrayList<>(readyBar.getPlayers())) {
            if (!players.contains(shown)) {
                readyBar.removePlayer(shown);
            }
        }

        for (Player player : players) {
            if (!readyBar.getPlayers().contains(player)) {
                readyBar.addPlayer(player);
            }
        }
    }

    private List<Player> onlineLobbyPlayers(LobbySnapshot snapshot) {
        List<Player> players = new ArrayList<>();

        for (PlayerId playerId : snapshot.players()) {
            Player player = plugin.getServer().getPlayer(playerId.value());
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }

        return players;
    }

    private boolean isInsideReadyZone(Location location) {
        if (location.getWorld() != world) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= READY_MIN_X
                && x <= READY_MAX_X
                && z >= READY_MIN_Z
                && z <= READY_MAX_Z
                && y >= FLOOR_Y + 1
                && y <= FLOOR_Y + 4;
    }

    private World resolveWorld() {
        World existing = Bukkit.getWorld(PaperShipWorldService.WORLD_NAME);
        if (existing != null) {
            return existing;
        }

        WorldCreator creator = new WorldCreator(PaperShipWorldService.WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);

        World created = creator.createWorld();
        if (created == null) {
            throw new IllegalStateException(
                    "Could not create lobby world: " + PaperShipWorldService.WORLD_NAME
            );
        }

        created.setDifficulty(Difficulty.PEACEFUL);
        created.setTime(6000L);
        return created;
    }

    private static void renderLobby(World world) {
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int z = MIN_Z; z <= MAX_Z; z++) {
                boolean boundary = x == MIN_X
                        || x == MAX_X
                        || z == MIN_Z
                        || z == MAX_Z;
                boolean ready = x >= READY_MIN_X
                        && x <= READY_MAX_X
                        && z >= READY_MIN_Z
                        && z <= READY_MAX_Z;

                Material floor;
                if (ready) {
                    floor = Material.LIME_CONCRETE;
                } else if (x == -80) {
                    floor = Material.YELLOW_CONCRETE;
                } else {
                    floor = Material.GRAY_CONCRETE;
                }

                world.getBlockAt(x, FLOOR_Y, z).setType(floor, false);

                for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 4; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                }

                if (boundary) {
                    world.getBlockAt(x, FLOOR_Y + 1, z)
                            .setType(Material.LIGHT_BLUE_STAINED_GLASS, false);
                    world.getBlockAt(x, FLOOR_Y + 2, z)
                            .setType(Material.LIGHT_BLUE_STAINED_GLASS, false);
                }
            }
        }

        int[][] lights = {
                {MIN_X + 1, MIN_Z + 1},
                {MIN_X + 1, MAX_Z - 1},
                {MAX_X - 1, MIN_Z + 1},
                {MAX_X - 1, MAX_Z - 1}
        };

        for (int[] light : lights) {
            world.getBlockAt(light[0], FLOOR_Y, light[1])
                    .setType(Material.SEA_LANTERN, false);
        }
    }
}
