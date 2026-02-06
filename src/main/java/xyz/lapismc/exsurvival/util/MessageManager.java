package xyz.lapismc.exsurvival.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DefaultQualifier(NonNull.class)
public class MessageManager {
    private final Map<String, FileConfiguration> messages = new HashMap<>();
    private final String defaultLocale = "zh_CN";
    private final File dataFolder;

    public MessageManager(File dataFolder) {
        this.dataFolder = dataFolder;
        loadMessages();
    }

    private void loadMessages() {
        // 加载中文和英文语言文件
        loadLocale("zh_CN");
        loadLocale("en_US");
    }

    private void loadLocale(String locale) {
        String fileName = "messages_" + locale + ".yml";
        File file = new File(dataFolder, fileName);

        FileConfiguration config;
        if (file.exists()) {
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            // 从资源文件加载
            InputStream stream = getClass().getClassLoader().getResourceAsStream(fileName);
            if (stream != null) {
                config = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
            } else {
                return;
            }
        }

        messages.put(locale, config);
    }

    public String getMessage(String key) {
        return getMessage(defaultLocale, key);
    }

    public String getMessage(String locale, String key) {
        FileConfiguration config = messages.getOrDefault(locale, messages.get(defaultLocale));
        if (config == null) {
            return key;
        }
        return config.getString(key, key);
    }

    public String getMessage(String key, Map<String, String> replacements) {
        return getMessage(defaultLocale, key, replacements);
    }

    public String getMessage(String locale, String key, Map<String, String> replacements) {
        String message = getMessage(locale, key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public List<String> getMessageList(String key) {
        return getMessageList(defaultLocale, key);
    }

    public List<String> getMessageList(String locale, String key) {
        FileConfiguration config = messages.getOrDefault(locale, messages.get(defaultLocale));
        if (config == null) {
            return List.of(key);
        }
        return config.getStringList(key);
    }

    public String getPlayerLocale(Player player) {
        String locale = player.locale().toString();
        // 如果玩家的语言文件存在，使用玩家的语言，否则使用默认语言
        return messages.containsKey(locale) ? locale : defaultLocale;
    }

    public void sendMessage(Player player, String key) {
        String locale = getPlayerLocale(player);
        player.sendMessage(getMessage(locale, key));
    }

    public void sendMessage(Player player, String key, Map<String, String> replacements) {
        String locale = getPlayerLocale(player);
        player.sendMessage(getMessage(locale, key, replacements));
    }

    public void sendMessageList(Player player, String key) {
        String locale = getPlayerLocale(player);
        List<String> messages = getMessageList(locale, key);
        for (String message : messages) {
            player.sendMessage(message);
        }
    }

    public void sendMessageList(Player player, String key, Map<String, String> replacements) {
        String locale = getPlayerLocale(player);
        List<String> messages = getMessageList(locale, key);
        for (String message : messages) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            player.sendMessage(message);
        }
    }

    public void reloadMessages() {
        messages.clear();
        loadMessages();
    }
}
