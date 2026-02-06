package xyz.lapismc.exsurvival.game;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@DefaultQualifier(NonNull.class)
public class PlayerDataManager {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public void savePlayerData(Player player) {
        PlayerData data = new PlayerData(
                player.getInventory().getContents(),
                player.getRespawnLocation(),
                player.getLocation(),
                player.getGameMode(),
                player.getHealth(),
                player.getAttribute(Attribute.MAX_HEALTH).getBaseValue(),
                player.getFoodLevel(),
                player.getLevel(),
                player.getExp()
        );
        playerDataMap.put(player.getUniqueId(), data);
    }

    public void restorePlayerData(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            player.getInventory().setContents(data.inventory());
            player.setRespawnLocation(data.spawnLocation, true);
            player.teleport(data.location());
            player.setGameMode(data.gameMode());
            player.setHealth(Math.min(data.health(), player.getAttribute(Attribute.MAX_HEALTH).getBaseValue()));
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(data.maxHealth());
            player.setFoodLevel(data.foodLevel());
            player.setLevel(data.level());
            player.setExp(data.exp());
            playerDataMap.remove(player.getUniqueId());
        }
    }

    public void clearPlayerData(Player player) {
        player.getInventory().clear();
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.setLevel(0);
        player.setExp(0);
    }

    public boolean hasData(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }

    public void clear() {
        playerDataMap.clear();
    }

    private record PlayerData(
            ItemStack[] inventory,
            Location spawnLocation,
            Location location,
            GameMode gameMode,
            double health,
            double maxHealth,
            int foodLevel,
            int level,
            float exp
    ) {
    }
}
