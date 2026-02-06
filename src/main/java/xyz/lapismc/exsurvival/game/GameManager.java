package xyz.lapismc.exsurvival.game;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.lapismc.exsurvival.EXSurvivalPlugin;
import xyz.lapismc.exsurvival.util.MessageManager;

import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

@DefaultQualifier(NonNull.class)
public class GameManager {
    private final EXSurvivalPlugin plugin;
    private final MessageManager messageManager;
    private final PlayerDataManager playerDataManager;
    private final WeaponSelectionMenu weaponMenu;
    private final GameSession session;
    private final ScoreboardManager scoreboardManager;
    private final Logger logger;

    private @Nullable BukkitTask countdownTask;
    private @Nullable BukkitTask gameTask;
    private @Nullable BossBar bossBar;
    private boolean shouldDisableMove = false;

    public GameManager(EXSurvivalPlugin plugin, MessageManager messageManager, Logger logger) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.playerDataManager = new PlayerDataManager();
        this.weaponMenu = new WeaponSelectionMenu(plugin, messageManager);
        this.session = new GameSession(logger);
        this.scoreboardManager = new ScoreboardManager(messageManager, session);
        this.logger = logger;
    }

    public boolean shouldDisableMove() {
        return shouldDisableMove;
    }

    public void setDisableMove(boolean value) {
        shouldDisableMove = value;
        logger.info("Move: " + (value ? "enabled" : "disabled"));
    }

    public boolean isGameRunning() {
        return session.getState() != GameState.IDLE;
    }

    public GameSession getSession() {
        return session;
    }

    public WeaponSelectionMenu getWeaponMenu() {
        return weaponMenu;
    }

    public void startGame(String worldId) {
        int maxHealth = plugin.getConfig().getInt("opening-phase.max-health", 40);

        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }

        session.setWorldId(worldId);
        session.setState(GameState.WEAPON_SELECTION);
        world.setDifficulty(Difficulty.PEACEFUL);

        // 保存所有在线玩家的数据
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.savePlayerData(player);
            playerDataManager.clearPlayerData(player);
            player.setGameMode(GameMode.SURVIVAL);
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);

            session.addParticipant(player);

            // 传送到世界出生点
            Location spawnLocation = world.getSpawnLocation();
            player.teleport(spawnLocation);
            player.setRespawnLocation(spawnLocation, true);

            // 给予说明书
            ItemStack guidebook = weaponMenu.createGuidebook(player);
            player.getInventory().addItem(guidebook);

            // 显示规则
            String locale = messageManager.getPlayerLocale(player);
            int borderSize = plugin.getConfig().getInt("opening-battle.world-border-size", 100);
            Map<String, String> replacements = Map.of("size", String.valueOf(borderSize));
            messageManager.sendMessageList(player, "opening-battle.rules", replacements);

            // 显示标题
            String title = messageManager.getMessage(locale, "opening-battle.title");
            String subtitle = messageManager.getMessage(locale, "opening-battle.subtitle");
            player.showTitle(Title.title(
                    Component.text(title),
                    Component.text(subtitle),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))));
        }

        session.setPlayerInteractivity(false);
    }

    public void onWeaponSelected(Player player, String weaponId) {
        UUID uuid = player.getUniqueId();
        session.setWeaponSelection(uuid, weaponId);

        // String locale = messageManager.getPlayerLocale(player);
        String weaponName = plugin.getConfig().getString("opening-battle.weapon-kits." + weaponId + ".name", weaponId);
        Map<String, String> replacements = Map.of("weapon", weaponName);
        messageManager.sendMessage(player, "opening-battle.weapon-selection.confirmed", replacements);
    }

    public void onPlayerReady(Player player) {
        UUID uuid = player.getUniqueId();
        session.setReady(uuid, true);

        int ready = session.getReadyCount();
        int total = session.getParticipants().size();

        // 通知所有玩家
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                Map<String, String> replacements = Map.of(
                        "ready", String.valueOf(ready),
                        "total", String.valueOf(total)
                );
                messageManager.sendMessage(participant, "opening-battle.weapon-selection.waiting", replacements);
            }
        }

        // 检查是否所有玩家都准备好了
        if (session.allPlayersReady()) {
            startOpeningBattle();
        }
    }

    private void startOpeningBattle() {
        session.setState(GameState.OPENING_BATTLE);

        // 通知所有玩家
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                messageManager.sendMessage(participant, "opening-battle.weapon-selection.all-ready");
            }
        }

        // 设置世界边界
        World world = Bukkit.getWorld(session.getWorldId());
        if (world != null) {
            int borderSize = plugin.getConfig().getInt("opening-battle.world-border-size", 100);
            Location center = world.getSpawnLocation();
            WorldBorder border = world.getWorldBorder();
            border.setCenter(center);
            border.setSize(borderSize);
        }

        // 分发武器并传送玩家
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null && world != null) {
                // 清空背包
                participant.getInventory().clear();

                // 装备防具
                weaponMenu.equipArmor(participant);

                // 给予选择的武器
                String weaponId = session.getWeaponSelection(participantId);
                if (weaponId != null) {
                    ItemStack weapon = weaponMenu.createWeaponKit(weaponId);
                    participant.getInventory().addItem(weapon);

                    // 给予额外物品
                    List<ItemStack> extraItems = weaponMenu.getExtraItems(weaponId);
                    for (ItemStack item : extraItems) {
                        participant.getInventory().addItem(item);
                    }
                }

                // 传送到出生点
                participant.teleport(world.getSpawnLocation());
            }
        }

        session.setPlayerInteractivity(false);
        // 倒计时5秒
        startCountdown(5, () -> {
            session.setPlayerInteractivity(true);

            // 显示开始消息
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    String locale = messageManager.getPlayerLocale(participant);
                    String title = messageManager.getMessage(locale, "opening-battle.start");
                    participant.showTitle(Title.title(
                            Component.text(title),
                            Component.empty(),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                    ));
                }
            }

            // 创建Boss栏
            createBossBar("bossbar.opening-battle");

            // 开始监控玩家死亡
            startOpeningBattleMonitor();
        });
    }

    private void startCountdown(int seconds, Runnable onComplete) {
        countdownTask = new BukkitRunnable() {
            int remaining = seconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    onComplete.run();
                    cancel();
                    return;
                }

                for (UUID participantId : session.getParticipants()) {
                    Player participant = Bukkit.getPlayer(participantId);
                    if (participant != null) {
                        Map<String, String> replacements = Map.of("seconds", String.valueOf(remaining));
                        String message;
                        if (session.getState() == GameState.OPENING_BATTLE) {
                            message = messageManager.getMessage(messageManager.getPlayerLocale(participant),
                                    "opening-battle.countdown", replacements);
                        } else {
                            message = messageManager.getMessage(messageManager.getPlayerLocale(participant),
                                    "survival-phase.countdown", replacements);
                        }
                        participant.sendMessage(message);
                    }
                }

                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startOpeningBattleMonitor() {
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> alive = session.getAliveParticipants();

                // 更新Boss栏
                if (bossBar != null) {
                    Map<String, String> replacements = Map.of("players", String.valueOf(alive.size()));
                    String message = messageManager.getMessage("bossbar.opening-battle", replacements);
                    bossBar.name(Component.text(message));
                    bossBar.progress(Math.max(0.0f, (float) alive.size() / session.getParticipants().size()));
                }

                // 检查是否只剩一名玩家
                if (alive.size() <= 1) {
                    endOpeningBattle();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void endOpeningBattle() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }

        // 隐藏Boss栏
        if (bossBar != null) {
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    participant.hideBossBar(bossBar);
                }
            }
            bossBar = null;
        }

        World world = Bukkit.getWorld(session.getWorldId());
        session.setPlayerInteractivity(false);

        // 复活所有玩家
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                participant.setGameMode(GameMode.SURVIVAL);
                participant.setHealth(participant.getAttribute(Attribute.MAX_HEALTH).getBaseValue());

                // 恢复死前装备
                ItemStack[] deathInventory = session.getDeathInventory(participantId);
                if (deathInventory != null) {
                    participant.getInventory().setContents(deathInventory);
                }

                if (world != null) {
                    participant.teleport(world.getSpawnLocation());
                }
            }
        }

        // 分发奖励
        distributeOpeningBattleRewards();

        // 15秒后进入正式游戏
        new BukkitRunnable() {
            @Override
            public void run() {
                startSurvivalPhase();
            }
        }.runTaskLater(plugin, 300L);
    }

    private void distributeOpeningBattleRewards() {
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant == null)
                continue;
            playerDataManager.clearPlayerData(participant);

            int rank = session.getRank(participantId);
            String rewardPath;

            if (rank == 1) {
                rewardPath = "opening-battle.survival-rewards.rank-1";
            } else if (rank == 2) {
                rewardPath = "opening-battle.survival-rewards.rank-2";
            } else if (rank == 3) {
                rewardPath = "opening-battle.survival-rewards.rank-3";
            } else {
                rewardPath = "opening-battle.survival-rewards.rank-other";
            }

            // 给予奖励
            List<Map<?, ?>> items = plugin.getConfig().getMapList(rewardPath + ".items");
            for (Map<?, ?> itemMap : items) {
                String type = (String) itemMap.get("type");
                Object amountObj = itemMap.get("amount");
                int amount = 1; // 默认值
                if (amountObj instanceof Integer) {
                    amount = (Integer) amountObj;
                } else if (amountObj instanceof String) {
                    try {
                        amount = Integer.parseInt((String) amountObj);
                    } catch (NumberFormatException e) {
                        amount = 1;
                    }
                }
                Material material = Material.getMaterial(type);
                if (material != null) {
                    participant.getInventory().addItem(new ItemStack(material, amount));
                }
            }

            // 发送消息
            String rankName = rank <= 3 ? "第" + rank + "名" : "其他";
            Map<String, String> replacements = Map.of("rank", rankName);
            messageManager.sendMessage(participant, "opening-battle.reward-received", replacements);
        }

        // 通知所有玩家奖励已分发
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                messageManager.sendMessage(participant, "opening-battle.rewards-distributed");
            }
        }
    }

    private void startSurvivalPhase() {
        session.setState(GameState.FREE_SURVIVAL);
        session.setGameStartTime(System.currentTimeMillis());

        World world = Bukkit.getWorld(session.getWorldId());
        if (world != null) {
            // 重置世界边界
            WorldBorder border = world.getWorldBorder();
            border.setSize(59999968); // 默认大小
            world.setDifficulty(Difficulty.EASY);
        }
        session.setPlayerInteractivity(false);

        int maxHealth = plugin.getConfig().getInt("survival-phase.max-health", 40);
        int freeSurvivalTime = plugin.getConfig().getInt("survival-phase.free-survival-time", 30);
        int totalGameTime = plugin.getConfig().getInt("survival-phase.total-game-time", 120);
        int maxDeaths = plugin.getConfig().getInt("survival-phase.max-deaths", 5);

        // 设置玩家血量并传送
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                // 设置最大血量
                var maxHealthAttr = participant.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(maxHealth);
                }
                participant.setHealth(maxHealth);

                // 传送到出生点
                if (world != null) {
                    participant.teleport(world.getSpawnLocation());
                }

                // 显示规则
                String locale = messageManager.getPlayerLocale(participant);
                Map<String, String> replacements = Map.of(
                        "free-time", String.valueOf(freeSurvivalTime),
                        "max-deaths", String.valueOf(maxDeaths),
                        "total-time", String.valueOf(totalGameTime),
                        "max-health", String.valueOf(maxHealth)
                );
                messageManager.sendMessageList(participant, "survival-phase.rules", replacements);

                // 显示标题
                String title = messageManager.getMessage(locale, "survival-phase.title");
                String subtitle = messageManager.getMessage(locale, "survival-phase.subtitle",
                        Map.of("time", String.valueOf(freeSurvivalTime)));
                participant.showTitle(Title.title(
                        Component.text(title),
                        Component.text(subtitle),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                ));
            }
        }

        setDisableMove(true);
        // 倒计时5秒
        startCountdown(5, () -> {
            setDisableMove(false);
            // 显示开始消息
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    messageManager.sendMessage(participant, "survival-phase.start");
                    participant.setGameMode(GameMode.SURVIVAL);
                }
            }

            // 创建Boss栏
            createBossBar("bossbar.free-survival");

            // 开始游戏计时器
            startSurvivalPhaseTimer();
        });
    }

    private void startSurvivalPhaseTimer() {
        int freeSurvivalMinutes = plugin.getConfig().getInt("survival-phase.free-survival-time", 30);
        int totalGameMinutes = plugin.getConfig().getInt("survival-phase.total-game-time", 120);

        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                long elapsedMillis = System.currentTimeMillis() - session.getGameStartTime();
                long elapsedMinutes = elapsedMillis / 60000;

                // 检查是否到达PvP时间
                if (session.getState() == GameState.FREE_SURVIVAL && elapsedMinutes >= freeSurvivalMinutes) {
                    enablePvP();
                }

                // 更新Boss栏
                updateSurvivalBossBar();

                // 检查是否游戏结束
                if (elapsedMinutes >= totalGameMinutes) {
                    endGame();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void enablePvP() {
        session.setState(GameState.PVP_PHASE);
        session.setPvpStartTime(System.currentTimeMillis());

        World world = Bukkit.getWorld(session.getWorldId());
        if (world != null) {
            world.setGameRule(GameRules.PVP, true);
        }

        // 为所有玩家创建记分板
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                scoreboardManager.createScoreboard(participant);
                scoreboardManager.updateScoreboard(participant);
            }
        }

        // 通知所有玩家
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                String locale = messageManager.getPlayerLocale(participant);
                String title = messageManager.getMessage(locale, "survival-phase.pvp-enabled-title");
                String subtitle = messageManager.getMessage(locale, "survival-phase.pvp-enabled-subtitle");
                participant.showTitle(Title.title(
                        Component.text(title),
                        Component.text(subtitle),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                ));
                messageManager.sendMessage(participant, "survival-phase.pvp-enabled");
            }
        }
    }

    private void updateSurvivalBossBar() {
        if (bossBar == null) return;

        long elapsedMillis = System.currentTimeMillis() - session.getGameStartTime();
        int totalGameMinutes = plugin.getConfig().getInt("survival-phase.total-game-time", 120);
        long totalMillis = totalGameMinutes * 60000L;
        long remainingMillis = totalMillis - elapsedMillis;

        if (remainingMillis < 0) remainingMillis = 0;

        long remainingMinutes = remainingMillis / 60000;
        long remainingSeconds = (remainingMillis % 60000) / 1000;
        String timeStr = String.format("%d:%02d", remainingMinutes, remainingSeconds);

        String messageKey = session.getState() == GameState.FREE_SURVIVAL ?
                "bossbar.free-survival" : "bossbar.pvp-phase";
        Map<String, String> replacements = Map.of("time", timeStr);
        String message = messageManager.getMessage(messageKey, replacements);

        bossBar.name(Component.text(message));
        bossBar.progress(Math.max(0.0f, Math.min(1.0f, (float) remainingMillis / totalMillis)));
    }

    private void endGame() {
        session.setState(GameState.ENDING);

        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }

        session.setPlayerInteractivity(false);

        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                messageManager.sendMessage(participant, "survival-phase.game-ending");
            }
        }

        // 显示排名
        new BukkitRunnable() {
            @Override
            public void run() {
                displayFinalRanking();
                restoreAllPlayers();
                cleanup();
            }
        }.runTaskLater(plugin, 100L);
    }

    private void displayFinalRanking() {
        // 按死亡次数排序
        List<Map.Entry<UUID, Integer>> ranking = new ArrayList<>();
        for (UUID participantId : session.getParticipants()) {
            ranking.add(Map.entry(participantId, session.getDeathCount(participantId)));
        }
        ranking.sort(Map.Entry.comparingByValue());

        // 显示排名
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                String locale = messageManager.getPlayerLocale(participant);

                // 显示标题
                String title = messageManager.getMessage(locale, "game-end.title");
                String subtitle = messageManager.getMessage(locale, "game-end.subtitle");
                participant.showTitle(Title.title(
                        Component.text(title),
                        Component.text(subtitle),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                ));

                // 显示排名列表
                participant.sendMessage(messageManager.getMessage(locale, "game-end.ranking-header"));
                int rank = 1;
                for (Map.Entry<UUID, Integer> entry : ranking) {
                    Player rankedPlayer = Bukkit.getPlayer(entry.getKey());
                    if (rankedPlayer != null) {
                        Map<String, String> replacements = Map.of(
                                "rank", String.valueOf(rank),
                                "player", rankedPlayer.getName(),
                                "deaths", String.valueOf(entry.getValue())
                        );
                        String message = messageManager.getMessage(locale, "game-end.ranking-entry", replacements);
                        participant.sendMessage(message);
                    }
                    rank++;
                }
                participant.sendMessage(messageManager.getMessage(locale, "game-end.ranking-footer"));

                // 显示获胜者
                if (!ranking.isEmpty()) {
                    Player winner = Bukkit.getPlayer(ranking.get(0).getKey());
                    if (winner != null) {
                        Map<String, String> replacements = Map.of("player", winner.getName());
                        String message = messageManager.getMessage(locale, "game-end.winner", replacements);
                        participant.sendMessage(message);
                    }
                }
            }
        }
    }

    private void restoreAllPlayers() {
        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                playerDataManager.restorePlayerData(participant);
            }
        }
    }

    private void cleanup() {
        if (bossBar != null) {
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    participant.hideBossBar(bossBar);
                }
            }
            bossBar = null;
        }

        scoreboardManager.clearAll();
        session.reset();
    }

    private void createBossBar(String messageKey) {
        if (bossBar != null) {
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    participant.hideBossBar(bossBar);
                }
            }
        }

        String message = messageManager.getMessage(messageKey);
        bossBar = BossBar.bossBar(
                Component.text(message),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );

        for (UUID participantId : session.getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null) {
                participant.showBossBar(bossBar);
            }
        }
    }

    public void handlePlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();

        if (session.getState() == GameState.OPENING_BATTLE) {
            // 开局战死亡
            session.addToEliminationOrder(uuid);
            session.eliminate(uuid);

            // 保存死前装备
            session.saveDeathInventory(uuid, player.getInventory().getContents());

            // 切换到观战模式
            player.setGameMode(GameMode.SPECTATOR);

            // 通知死亡排名
            int rank = session.getRank(uuid);
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    Map<String, String> replacements = Map.of(
                            "player", player.getName(),
                            "rank", String.valueOf(rank)
                    );
                    messageManager.sendMessage(participant, "opening-battle.death-order", replacements);
                }
            }
        } else if (session.getState() == GameState.PVP_PHASE) {
            // PvP阶段死亡
            session.incrementDeathCount(uuid);
            int deaths = session.getDeathCount(uuid);
            int maxDeaths = plugin.getConfig().getInt("survival-phase.max-deaths", 5);

            // 更新记分板
            scoreboardManager.updateScoreboard(player);

            Map<String, String> replacements = Map.of(
                    "deaths", String.valueOf(deaths),
                    "max-deaths", String.valueOf(maxDeaths)
            );
            messageManager.sendMessage(player, "survival-phase.death-message", replacements);

            if (deaths >= maxDeaths) {
                // 玩家被淘汰
                session.eliminate(uuid);
                player.setGameMode(GameMode.SPECTATOR);
                messageManager.sendMessage(player, "survival-phase.eliminated");

                // 通知所有玩家
                for (UUID participantId : session.getParticipants()) {
                    Player participant = Bukkit.getPlayer(participantId);
                    if (participant != null && !participant.equals(player)) {
                        Map<String, String> notifyReplacements = Map.of("player", player.getName());
                        messageManager.sendMessage(participant, "survival-phase.player-eliminated", notifyReplacements);
                    }
                }
            }
        }
    }

    public void stopGame() {
        // 取消所有任务
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }

        // 恢复所有玩家
        restoreAllPlayers();

        // 清理资源
        cleanup();
    }

    public void handlePlayerDisconnect(Player player) {
        UUID uuid = player.getUniqueId();

        // 检查玩家是否在游戏中
        if (!session.getParticipants().contains(uuid)) {
            return;
        }

        GameState state = session.getState();

        if (state == GameState.WEAPON_SELECTION) {
            // 武器选择阶段，移除玩家
            session.removeParticipant(uuid);
            playerDataManager.restorePlayerData(player);

            // 通知其他玩家
            for (UUID participantId : session.getParticipants()) {
                Player participant = Bukkit.getPlayer(participantId);
                if (participant != null) {
                    participant.sendMessage("§c玩家 " + player.getName() + " 离线了！");
                }
            }

            // 如果没有玩家了，停止游戏
            if (session.getParticipants().isEmpty()) {
                stopGame();
            }
        } else if (state == GameState.OPENING_BATTLE || state == GameState.FREE_SURVIVAL
                || state == GameState.PVP_PHASE) {
            // 游戏进行中，玩家离线则自动淘汰
            if (!session.isEliminated(uuid)) {
                session.eliminate(uuid);
                session.addToEliminationOrder(uuid);

                // 通知其他玩家
                for (UUID participantId : session.getParticipants()) {
                    Player participant = Bukkit.getPlayer(participantId);
                    if (participant != null && !participant.equals(player)) {
                        participant.sendMessage("§c玩家 " + player.getName() + " 离线了！");
                    }
                }

                // 检查是否游戏应该结束
                Set<UUID> alive = session.getAliveParticipants();
                if (alive.size() <= 1) {
                    if (state == GameState.OPENING_BATTLE) {
                        endOpeningBattle();
                    } else {
                        endGame();
                    }
                }
            }
        }
    }

    public void handlePlayerRejoin(Player player) {
        UUID uuid = player.getUniqueId();

        GameState state = session.getState();

        // 如果玩家在游戏进行中重新加入，恢复其状态
        if (session.getParticipants().contains(uuid) && (state == GameState.OPENING_BATTLE
                || state == GameState.FREE_SURVIVAL || state == GameState.PVP_PHASE)) {
            World world = Bukkit.getWorld(session.getWorldId());
            if (world != null) {
                // 如果玩家已被淘汰，设置为观战模式
                if (session.isEliminated(uuid)) {
                    player.setGameMode(GameMode.SPECTATOR);
                } else {
                    player.setGameMode(GameMode.SURVIVAL);

                    // 恢复玩家血量
                    if (state == GameState.FREE_SURVIVAL || state == GameState.PVP_PHASE) {
                        int maxHealth = plugin.getConfig().getInt("survival-phase.max-health", 40);
                        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                        if (maxHealthAttr != null) {
                            maxHealthAttr.setBaseValue(maxHealth);
                        }
                        player.setHealth(maxHealth);
                    }
                }

                // 传送到世界出生点
                player.teleport(world.getSpawnLocation());

                // 通知玩家
                player.sendMessage("§a你已重新加入游戏！");
            }
        }

        if ((state == GameState.IDLE || state == GameState.ENDING) && playerDataManager.hasData(player.getUniqueId())) {
            playerDataManager.restorePlayerData(player);
        }
    }

    public void handlePlayerMove(PlayerMoveEvent event) {
        if (!session.getParticipants().contains(event.getPlayer().getUniqueId())) {
            return;
        }
        GameState state = session.getState();
        if (state == GameState.IDLE) {
            return;
        }
        if (shouldDisableMove() && event.hasChangedBlock()) {
            event.setCancelled(true);
        }
    }
}
