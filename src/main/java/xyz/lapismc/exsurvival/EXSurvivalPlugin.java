package xyz.lapismc.exsurvival;

import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.lapismc.exsurvival.command.EXSurvivalCommand;
import xyz.lapismc.exsurvival.game.GameManager;
import xyz.lapismc.exsurvival.listener.GameListener;
import xyz.lapismc.exsurvival.util.MessageManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@DefaultQualifier(NonNull.class)
public class EXSurvivalPlugin extends JavaPlugin {
    private MessageManager messageManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        // 创建数据文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 保存默认配置文件
        saveDefaultConfig();
        saveResource("messages_zh_CN.yml", false);
        saveResource("messages_en_US.yml", false);

        // 初始化管理器
        messageManager = new MessageManager(getDataFolder());
        gameManager = new GameManager(this, messageManager, getLogger());

        // 注册命令
        var exsCommand = getCommand("exsurvival");
        if (exsCommand != null) {
            EXSurvivalCommand commandExecutor = new EXSurvivalCommand(this, messageManager, gameManager);
            exsCommand.setExecutor(commandExecutor);
            exsCommand.setTabCompleter(commandExecutor);
        }

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new GameListener(this, messageManager, gameManager), this);

        getLogger().info("EXSurvival 插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("EXSurvival 插件已禁用！");
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
