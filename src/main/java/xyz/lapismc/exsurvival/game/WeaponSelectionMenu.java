package xyz.lapismc.exsurvival.game;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import xyz.lapismc.exsurvival.EXSurvivalPlugin;
import xyz.lapismc.exsurvival.util.MessageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DefaultQualifier(NonNull.class)
public class WeaponSelectionMenu {
    private final EXSurvivalPlugin plugin;
    private final MessageManager messageManager;

    public WeaponSelectionMenu(EXSurvivalPlugin plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    public void openMenu(Player player) {
        String locale = messageManager.getPlayerLocale(player);
        String title = messageManager.getMessage(locale, "opening-battle.weapon-selection.menu-title");

        Inventory inventory = Bukkit.createInventory(null, 27, Component.text(title));

        // 添加武器选项
        addWeaponOption(inventory, 10, "netherite-spear", locale);
        addWeaponOption(inventory, 11, "netherite-axe", locale);
        addWeaponOption(inventory, 12, "heavy-hammer", locale);
        addWeaponOption(inventory, 13, "bow", locale);
        addWeaponOption(inventory, 14, "trident", locale);
        addWeaponOption(inventory, 15, "crossbow", locale);

        // 添加确认按钮
        ItemStack confirmButton = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmButton.getItemMeta();
        confirmMeta.displayName(Component.text(messageManager.getMessage(locale, "opening-battle.weapon-selection.confirm-button")));
        confirmButton.setItemMeta(confirmMeta);
        inventory.setItem(22, confirmButton);

        player.openInventory(inventory);
    }

    private void addWeaponOption(Inventory inventory, int slot, String weaponId, String locale) {
        String path = "opening-battle.weapon-kits." + weaponId;
        String name = plugin.getConfig().getString(path + ".name", weaponId);
        String materialName = plugin.getConfig().getString(path + ".item", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6" + name));

        List<String> lore = new ArrayList<>();
        List<String> configLore = plugin.getConfig().getStringList(path + ".lore");
        lore.addAll(configLore);
        lore.add("");
        lore.add("§e点击选择此武器");

        meta.lore(lore.stream().map(Component::text).toList());
        item.setItemMeta(meta);

        inventory.setItem(slot, item);
    }

    public ItemStack createWeaponKit(String weaponId) {
        String path = "opening-battle.weapon-kits." + weaponId;
        String materialName = plugin.getConfig().getString(path + ".item", "STONE");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            material = Material.STONE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString(path + ".name", weaponId);
        meta.displayName(Component.text("§6" + name));

        // 添加附魔
        var enchantments = plugin.getConfig().getConfigurationSection(path + ".enchantments");
        if (enchantments != null) {
            for (String enchantKey : enchantments.getKeys(false)) {
                Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(Key.key(enchantKey));
                if (enchantment != null) {
                    int level = enchantments.getInt(enchantKey, 1);
                    meta.addEnchant(enchantment, level, true);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public List<ItemStack> getExtraItems(String weaponId) {
        List<ItemStack> items = new ArrayList<>();
        String path = "opening-battle.weapon-kits." + weaponId + ".extra-items";

        var extraItemsSection = plugin.getConfig().getConfigurationSection(path);
        if (extraItemsSection == null) {
            // 尝试列表格式
            List<Map<?, ?>> extraItemsList = plugin.getConfig().getMapList(path);
            for (Map<?, ?> itemMap : extraItemsList) {
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
                    items.add(new ItemStack(material, amount));
                }
            }
        }

        return items;
    }

    public void equipArmor(Player player) {
        // 装备头盔
        ItemStack helmet = createArmorPiece("opening-battle.standard-armor.helmet");
        if (helmet != null) {
            player.getEquipment().setHelmet(helmet);
        }

        // 装备胸甲
        ItemStack chestplate = createArmorPiece("opening-battle.standard-armor.chestplate");
        if (chestplate != null) {
            player.getEquipment().setChestplate(chestplate);
        }

        // 装备腿甲
        ItemStack leggings = createArmorPiece("opening-battle.standard-armor.leggings");
        if (leggings != null) {
            player.getEquipment().setLeggings(leggings);
        }

        // 装备靴子
        ItemStack boots = createArmorPiece("opening-battle.standard-armor.boots");
        if (boots != null) {
            player.getEquipment().setBoots(boots);
        }

        // 添加其他装备
        List<Map<?, ?>> extraEquipment = plugin.getConfig().getMapList("opening-battle.extra-equipment");
        for (Map<?, ?> itemMap : extraEquipment) {
            String type = (String) itemMap.get("type");
            Object amountObj = itemMap.get("amount");
            int amount = 1;
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
                player.getInventory().addItem(new ItemStack(material, amount));
            }
        }
    }

    private ItemStack createArmorPiece(String path) {
        String materialName = plugin.getConfig().getString(path + ".type");
        if (materialName == null) {
            return null;
        }
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // 添加附魔
        var enchantments = plugin.getConfig().getConfigurationSection(path + ".enchantments");
        if (enchantments != null) {
            for (String enchantKey : enchantments.getKeys(false)) {
                Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                        .get(Key.key(enchantKey));
                if (enchantment != null) {
                    int level = enchantments.getInt(enchantKey, 1);
                    meta.addEnchant(enchantment, level, true);
                }
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createGuidebook(Player player) {
        String locale = messageManager.getPlayerLocale(player);
        String bookName = messageManager.getMessage(locale, "opening-battle.weapon-selection.book-name");

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.displayName(Component.text(bookName));
        book.setItemMeta(meta);

        return book;
    }
}
