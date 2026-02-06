package xyz.lapismc.exsurvival.game;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.lapismc.exsurvival.util.MessageManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@DefaultQualifier(NonNull.class)
public class ScoreboardManager {
    private final MessageManager messageManager;
    private final GameSession session;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    public ScoreboardManager(MessageManager messageManager, GameSession session) {
        this.messageManager = messageManager;
        this.session = session;
    }

    public void createScoreboard(Player player) {
        String locale = messageManager.getPlayerLocale(player);

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "exsurvival",
                Criteria.DUMMY,
                Component.text(messageManager.getMessage(locale, "scoreboard.title"))
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
    }

    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            return;
        }

        String locale = messageManager.getPlayerLocale(player);
        Objective objective = scoreboard.getObjective("exsurvival");
        if (objective == null) {
            return;
        }

        // 清除旧的分数
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        GameState state = session.getState();
        if (state == GameState.PVP_PHASE) {
            // 显示死亡次数
            String deathsLabel = messageManager.getMessage(locale, "scoreboard.deaths");
            int deaths = session.getDeathCount(player.getUniqueId());

            Score deathScore = objective.getScore(deathsLabel + ": " + deaths);
            deathScore.setScore(1);
        }
    }

    public void removeScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.remove(player.getUniqueId());
        if (scoreboard != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void updateAllScoreboards() {
        for (UUID uuid : session.getParticipants()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                updateScoreboard(player);
            }
        }
    }

    public void clearAll() {
        for (UUID uuid : playerScoreboards.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeScoreboard(player);
            }
        }
        playerScoreboards.clear();
    }
}
