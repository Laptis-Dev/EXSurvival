package xyz.lapismc.exsurvival.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import xyz.lapismc.exsurvival.EXSurvivalPlugin;
import xyz.lapismc.exsurvival.game.GameManager;
import xyz.lapismc.exsurvival.game.GameState;
import xyz.lapismc.exsurvival.util.MessageManager;

@DefaultQualifier(NonNull.class)
public class GameListener implements Listener {
    private final EXSurvivalPlugin plugin;
    private final MessageManager messageManager;
    private final GameManager gameManager;

    public GameListener(EXSurvivalPlugin plugin, MessageManager messageManager, GameManager gameManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.WRITTEN_BOOK) {
            return;
        }

        // 检查是否是游戏说明书
        if (gameManager.getSession().getState() == GameState.WEAPON_SELECTION) {
            String bookName = messageManager.getMessage(
                    messageManager.getPlayerLocale(player),
                    "opening-battle.weapon-selection.book-name"
            );

            if (item.getItemMeta() != null && item.getItemMeta().displayName() != null) {
                String itemName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(item.getItemMeta().displayName());

                // 移除格式代码进行比较
                String cleanBookName = bookName.replaceAll("§.", "");
                String cleanItemName = itemName.replaceAll("§.", "");

                if (cleanItemName.contains("EXSurvival") || cleanItemName.equals(cleanBookName)) {
                    event.setCancelled(true);
                    gameManager.getWeaponMenu().openMenu(player);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (gameManager.getSession().getState() != GameState.WEAPON_SELECTION) {
            return;
        }

        String locale = messageManager.getPlayerLocale(player);
        String menuTitle = messageManager.getMessage(locale, "opening-battle.weapon-selection.menu-title");
        String viewTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (!viewTitle.equals(menuTitle)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();

        // 检查是否点击了武器选项
        String weaponId = null;
        switch (slot) {
            case 10 -> weaponId = "netherite-spear";
            case 11 -> weaponId = "netherite-axe";
            case 12 -> weaponId = "heavy-hammer";
            case 13 -> weaponId = "bow";
            case 14 -> weaponId = "trident";
            case 15 -> weaponId = "crossbow";
            case 22 -> {
                // 确认按钮
                if (gameManager.getSession().getWeaponSelection(player.getUniqueId()) != null) {
                    gameManager.onPlayerReady(player);
                    player.closeInventory();
                }
                return;
            }
        }

        if (weaponId != null) {
            gameManager.onWeaponSelected(player, weaponId);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (!gameManager.getSession().getParticipants().contains(player.getUniqueId())) {
            return;
        }

        GameState state = gameManager.getSession().getState();
        if (state != GameState.IDLE) {
            gameManager.handlePlayerDeath(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        gameManager.handlePlayerDisconnect(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        gameManager.handlePlayerRejoin(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        gameManager.handlePlayerMove(event);
    }
}
