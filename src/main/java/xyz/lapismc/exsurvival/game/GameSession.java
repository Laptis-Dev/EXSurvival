package xyz.lapismc.exsurvival.game;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.*;
import java.util.logging.Logger;

@DefaultQualifier(NonNull.class)
public class GameSession {
    private final Set<UUID> participants = new HashSet<>();
    private final Map<UUID, String> weaponSelections = new HashMap<>();
    private final Map<UUID, Boolean> readyStatus = new HashMap<>();
    private final Map<UUID, Integer> deathCounts = new HashMap<>();
    private final Map<UUID, ItemStack[]> deathInventories = new HashMap<>();
    private final List<UUID> eliminationOrder = new ArrayList<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private final Logger logger;

    GameSession(Logger logger) {
        this.logger = logger;
    }

    private GameState state = GameState.IDLE;
    private String worldId;
    private long gameStartTime;
    private long pvpStartTime;

    public void addParticipant(Player player) {
        UUID uuid = player.getUniqueId();
        participants.add(uuid);
        readyStatus.put(uuid, false);
        deathCounts.put(uuid, 0);
    }

    public void removeParticipant(UUID uuid) {
        participants.remove(uuid);
        weaponSelections.remove(uuid);
        readyStatus.remove(uuid);
        deathCounts.remove(uuid);
        deathInventories.remove(uuid);
        eliminationOrder.remove(uuid);
        eliminated.remove(uuid);
    }

    public void setPlayerInteractivity(boolean interactivity) {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }

        world.setGameRule(GameRules.PVP, interactivity);
        for (UUID participantId : getParticipants()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant == null) {
                continue;
            }
            participant.setGameMode(interactivity ? GameMode.SURVIVAL : GameMode.ADVENTURE);
        }

        logger.info("Interactivity: " + Boolean.valueOf(interactivity).toString());
    }

    public Set<UUID> getParticipants() {
        return new HashSet<>(participants);
    }

    public void setWeaponSelection(UUID uuid, String weaponId) {
        weaponSelections.put(uuid, weaponId);
    }

    public @Nullable String getWeaponSelection(UUID uuid) {
        return weaponSelections.get(uuid);
    }

    public void setReady(UUID uuid, boolean ready) {
        readyStatus.put(uuid, ready);
    }

    public boolean isReady(UUID uuid) {
        return readyStatus.getOrDefault(uuid, false);
    }

    public boolean allPlayersReady() {
        return !participants.isEmpty() && readyStatus.values().stream().allMatch(ready -> ready);
    }

    public int getReadyCount() {
        return (int) readyStatus.values().stream().filter(ready -> ready).count();
    }

    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    public String getWorldId() {
        return worldId;
    }

    public void setWorldId(String worldId) {
        this.worldId = worldId;
    }

    public void incrementDeathCount(UUID uuid) {
        deathCounts.put(uuid, deathCounts.getOrDefault(uuid, 0) + 1);
    }

    public int getDeathCount(UUID uuid) {
        return deathCounts.getOrDefault(uuid, 0);
    }

    public void addToEliminationOrder(UUID uuid) {
        if (!eliminationOrder.contains(uuid)) {
            eliminationOrder.add(uuid);
        }
    }

    public List<UUID> getEliminationOrder() {
        return new ArrayList<>(eliminationOrder);
    }

    public int getRank(UUID uuid) {
        int index = eliminationOrder.indexOf(uuid);
        return index >= 0 ? eliminationOrder.size() - index : 1;
    }

    public void saveDeathInventory(UUID uuid, ItemStack[] inventory) {
        deathInventories.put(uuid, inventory);
    }

    public ItemStack @Nullable [] getDeathInventory(UUID uuid) {
        return deathInventories.get(uuid);
    }

    public void eliminate(UUID uuid) {
        eliminated.add(uuid);
    }

    public boolean isEliminated(UUID uuid) {
        return eliminated.contains(uuid);
    }

    public Set<UUID> getAliveParticipants() {
        Set<UUID> alive = new HashSet<>(participants);
        alive.removeAll(eliminated);
        return alive;
    }

    public void setGameStartTime(long time) {
        this.gameStartTime = time;
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public void setPvpStartTime(long time) {
        this.pvpStartTime = time;
    }

    public long getPvpStartTime() {
        return pvpStartTime;
    }

    public void reset() {
        participants.clear();
        weaponSelections.clear();
        readyStatus.clear();
        deathCounts.clear();
        deathInventories.clear();
        eliminationOrder.clear();
        eliminated.clear();
        state = GameState.IDLE;
        worldId = null;
        gameStartTime = 0;
        pvpStartTime = 0;
    }
}
