package xyz.lapismc.exsurvival.command;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.lapismc.exsurvival.EXSurvivalPlugin;
import xyz.lapismc.exsurvival.game.GameManager;
import xyz.lapismc.exsurvival.util.MessageManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DefaultQualifier(NonNull.class)
public class EXSurvivalCommand implements CommandExecutor, TabCompleter {
    private final EXSurvivalPlugin plugin;
    private final MessageManager messageManager;
    private final GameManager gameManager;

    public EXSurvivalCommand(EXSurvivalPlugin plugin, MessageManager messageManager, GameManager gameManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messageManager.getMessage("command.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("start")) {
            return handleStart(sender, args);
        } else if (subCommand.equals("stop")) {
            return handleStop(sender);
        } else if (subCommand.equals("reload")) {
            return handleReload(sender);
        }

        sender.sendMessage(messageManager.getMessage("command.usage"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        // 检查权限
        if (!sender.hasPermission("exsurvival.reload")) {
            sender.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }

        // 重载配置
        plugin.reloadConfig();
        messageManager.reloadMessages();

        sender.sendMessage("§a配置已重载！");
        return true;
    }

    private boolean handleStop(CommandSender sender) {
        // 检查权限
        if (!sender.hasPermission("exsurvival.control")) {
            sender.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }

        // 检查是否有游戏在运行
        if (!gameManager.isGameRunning()) {
            sender.sendMessage(messageManager.getMessage("no-game-running"));
            return true;
        }

        // 停止游戏
        gameManager.stopGame();
        sender.sendMessage("§a游戏已停止！");

        return true;
    }

    private boolean handleStart(CommandSender sender, String[] args) {
        // 检查权限
        if (!sender.hasPermission("exsurvival.control")) {
            sender.sendMessage(messageManager.getMessage("no-permission"));
            return true;
        }

        // 检查参数
        if (args.length < 2) {
            sender.sendMessage(messageManager.getMessage("command.usage"));
            return true;
        }

        // 检查是否已有游戏在运行
        if (gameManager.isGameRunning()) {
            sender.sendMessage(messageManager.getMessage("game-already-running"));
            return true;
        }

        String worldId = args[1];
        World world = Bukkit.getWorld(worldId);

        if (world == null) {
            Map<String, String> replacements = Map.of("world", worldId);
            sender.sendMessage(messageManager.getMessage("world-not-found", replacements));
            return true;
        }

        // 开始游戏
        gameManager.startGame(worldId);

        Map<String, String> replacements = Map.of("world", worldId);
        sender.sendMessage(messageManager.getMessage("command.game-started", replacements));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("start");
            completions.add("stop");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equals("start")) {
            // 建议所有世界名称
            for (World world : Bukkit.getWorlds()) {
                completions.add(world.getName());
            }
        }

        return completions;
    }
}
