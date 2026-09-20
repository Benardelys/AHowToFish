package com.ardelys.ahowtofish.language;

import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.player.ProfileManager;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final ProfileManager profileManager;
    private final Map<String, FileConfiguration> languageConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> flattenedMessages = new ConcurrentHashMap<>();
    private final Set<String> loggedMissingKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private volatile String defaultLanguage = "en";

    public LanguageManager(JavaPlugin plugin, ProfileManager profileManager) {
        this.plugin = plugin;
        this.profileManager = profileManager;
    }

    public static String normalize(String code) {
        if (code == null || code.isBlank()) return "en";
        return code.trim().toLowerCase(Locale.ROOT);
    }

    public void load(String configuredLang) {
        this.defaultLanguage = normalize(configuredLang);
        this.languageConfigs.clear();
        this.flattenedMessages.clear();
        this.loggedMissingKeys.clear();

        File dataFolder = plugin != null ? plugin.getDataFolder() : new File(".");
        File langFolder = new File(dataFolder, "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLangFile("en.yml");
        saveDefaultLangFile("tr.yml");

        File[] files = langFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String code = normalize(fileName.substring(0, fileName.lastIndexOf('.')));
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(reader);
                    languageConfigs.put(code, config);
                    flattenLanguage(code, config);
                } catch (Exception e) {
                    if (plugin != null && plugin.getLogger() != null) {
                        plugin.getLogger().severe("[AHowToFish] Failed to load language file '" + file.getName() + "': " + e.getMessage());
                    }
                }
            }
        }

        if (!languageConfigs.containsKey(defaultLanguage)) {
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().warning("[AHowToFish] WARNING: Language '" + defaultLanguage + "' is configured but lang/" + defaultLanguage + ".yml could not be loaded.");
                plugin.getLogger().warning("[AHowToFish] Falling back to English.");
            }
            this.defaultLanguage = "en";
        } else {
            if (plugin != null && plugin.getLogger() != null) {
                String displayName = defaultLanguage.equals("tr") ? "Turkish (tr)" : defaultLanguage.equals("en") ? "English (en)" : defaultLanguage;
                plugin.getLogger().info("[AHowToFish] Default language: " + displayName);
                plugin.getLogger().info("[AHowToFish] Loaded language file: lang/" + defaultLanguage + ".yml");
            }
        }

        validateTranslations();

        if (plugin != null && plugin.getLogger() != null) {
            plugin.getLogger().info("[AHowToFish] Language system initialized.");
            plugin.getLogger().info("[AHowToFish] Server default language: " + defaultLanguage);
            plugin.getLogger().info("[AHowToFish] Available languages: " + String.join(", ", languageConfigs.keySet()));
            plugin.getLogger().info("[AHowToFish] Loaded language: " + defaultLanguage);
        }
    }

    private void validateTranslations() {
        if (plugin == null || plugin.getLogger() == null) return;
        FileConfiguration enConfig = languageConfigs.get("en");
        if (enConfig == null) return;

        Set<String> enKeys = enConfig.getKeys(true);
        int enCount = 0;
        for (String k : enKeys) {
            if (!enConfig.isConfigurationSection(k)) enCount++;
        }

        for (Map.Entry<String, FileConfiguration> entry : languageConfigs.entrySet()) {
            if (entry.getKey().equals("en")) continue;
            FileConfiguration other = entry.getValue();
            int otherCount = 0;
            List<String> missing = new ArrayList<>();
            for (String k : enKeys) {
                if (!enConfig.isConfigurationSection(k)) {
                    if (other.contains(k)) {
                        otherCount++;
                    } else {
                        missing.add(k);
                    }
                }
            }

            String langName = entry.getKey().equals("tr") ? "Turkish" : entry.getKey().toUpperCase(Locale.ROOT);
            plugin.getLogger().info("[AHowToFish] Translation validation:");
            plugin.getLogger().info("  English keys: " + enCount);
            plugin.getLogger().info("  " + langName + " keys: " + otherCount);
            if (missing.isEmpty()) {
                plugin.getLogger().info("  Missing " + langName + " keys: 0");
            } else {
                plugin.getLogger().warning("  Missing " + langName + " keys: " + missing.size() + " (" + String.join(", ", missing.stream().limit(5).toList()) + (missing.size() > 5 ? "..." : "") + ")");
            }
        }
    }

    private void saveDefaultLangFile(String resourceName) {
        if (plugin == null) return;
        File file = new File(plugin.getDataFolder(), "lang/" + resourceName);
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("lang/" + resourceName)) {
                if (in != null) {
                    plugin.saveResource("lang/" + resourceName, false);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[AHowToFish] Could not save default " + resourceName + ": " + e.getMessage());
            }
        }
    }

    public String getPlayerLanguage(Player player) {
        if (player == null) return defaultLanguage;
        if (profileManager != null) {
            FishingProfile profile = profileManager.getProfile(player);
            if (profile != null && profile.hasExplicitLanguage()) {
                String lang = normalize(profile.getLanguage());
                if (languageConfigs.containsKey(lang)) {
                    return lang;
                }
            }
        }
        return defaultLanguage;
    }

    public String getPlayerLanguage(UUID playerUuid) {
        if (playerUuid == null) return defaultLanguage;
        if (profileManager != null) {
            FishingProfile profile = profileManager.getProfile(playerUuid);
            if (profile != null && profile.hasExplicitLanguage()) {
                String lang = normalize(profile.getLanguage());
                if (languageConfigs.containsKey(lang)) {
                    return lang;
                }
            }
        }
        return defaultLanguage;
    }

    public void setPlayerLanguage(UUID playerUuid, String language) {
        if (playerUuid == null || profileManager == null) return;
        FishingProfile profile = profileManager.getProfile(playerUuid);
        if (profile != null) {
            profile.setLanguage(language);
            profileManager.saveProfileAsync(profile);
        }
    }

    private void flattenLanguage(String code, FileConfiguration config) {
        Map<String, String> map = new ConcurrentHashMap<>();
        for (String key : config.getKeys(true)) {
            if (!config.isConfigurationSection(key)) {
                String val = config.getString(key, "");
                map.put(key, val);
            }
        }
        flattenedMessages.put(code, map);
    }

    public String getRawMessage(String langCode, String path) {
        String code = normalize(langCode);

        Map<String, String> map = flattenedMessages.get(code);
        if (map != null) {
            String val = map.get(path);
            if (val != null) return val;
        }

        if (!code.equals(defaultLanguage)) {
            map = flattenedMessages.get(defaultLanguage);
            if (map != null) {
                String val = map.get(path);
                if (val != null) return val;
            }
        }

        if (!code.equals("en") && !defaultLanguage.equals("en")) {
            map = flattenedMessages.get("en");
            if (map != null) {
                String val = map.get(path);
                if (val != null) return val;
            }
        }

        if (loggedMissingKeys.add(code + ":" + path)) {
            if (plugin != null && plugin.getLogger() != null) {
                plugin.getLogger().warning("[AHowToFish] Missing translation key '" + path + "' in language '" + code + "'.");
            }
        }

        return path;
    }

    public String getMessage(String langCode, MessageKey key, Object... replacements) {
        return getMessage(langCode, key.getPath(), replacements);
    }

    public String getMessage(String langCode, String path, Object... replacements) {
        String raw = getRawMessage(langCode, path);
        if (raw == null || raw.isEmpty()) return "";

        String prefix = getRawMessage(langCode, "prefix");
        if (raw.contains("{prefix}")) {
            raw = raw.replace("{prefix}", prefix != null ? prefix : "");
        }
        if (raw.contains("%prefix%")) {
            raw = raw.replace("%prefix%", prefix != null ? prefix : "");
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String target = String.valueOf(replacements[i]);
                String replacement = String.valueOf(replacements[i + 1]);
                raw = raw.replace("{" + target + "}", replacement);
                raw = raw.replace("%" + target + "%", replacement);
            }
        }

        return TextUtil.colorize(raw);
    }

    public String getMessage(CommandSender sender, MessageKey key, Object... replacements) {
        return getMessage(sender, key.getPath(), replacements);
    }

    public String getMessage(CommandSender sender, String path, Object... replacements) {
        String lang = defaultLanguage;
        if (sender instanceof Player player) {
            lang = getPlayerLanguage(player);
        }
        return getMessage(lang, path, replacements);
    }

    public String getMessage(Player player, MessageKey key, Object... replacements) {
        return getMessage(player, key.getPath(), replacements);
    }

    public String getMessage(Player player, String path, Object... replacements) {
        String lang = getPlayerLanguage(player);
        return getMessage(lang, path, replacements);
    }

    public String getMessage(Player player, String path, String defaultMessage) {
        String lang = getPlayerLanguage(player);
        return getMessageOrDefault(lang, path, defaultMessage);
    }

    public String getMessageOrDefault(String langCode, String path, String defaultMessage, Object... replacements) {
        String code = normalize(langCode);
        String raw = null;
        Map<String, String> map = flattenedMessages.get(code);
        if (map != null) {
            raw = map.get(path);
        }
        if (raw == null && !code.equals(defaultLanguage)) {
            map = flattenedMessages.get(defaultLanguage);
            if (map != null) {
                raw = map.get(path);
            }
        }
        if (raw == null && !code.equals("en") && !defaultLanguage.equals("en")) {
            map = flattenedMessages.get("en");
            if (map != null) {
                raw = map.get(path);
            }
        }
        if (raw == null) {
            FileConfiguration config = languageConfigs.get(code);
            if (config == null || !config.contains(path)) {
                if (!code.equals(defaultLanguage)) {
                    config = languageConfigs.get(defaultLanguage);
                }
            }
            if (config == null || !config.contains(path)) {
                if (!code.equals("en") && !defaultLanguage.equals("en")) {
                    config = languageConfigs.get("en");
                }
            }
            raw = (config != null && config.contains(path)) ? config.getString(path, defaultMessage) : defaultMessage;
        }
        if (raw == null || raw.isEmpty()) return "";

        String prefix = getRawMessage(langCode, "prefix");
        if (raw.contains("{prefix}")) {
            raw = raw.replace("{prefix}", prefix != null ? prefix : "");
        }
        if (raw.contains("%prefix%")) {
            raw = raw.replace("%prefix%", prefix != null ? prefix : "");
        }

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String target = String.valueOf(replacements[i]);
                String replacement = String.valueOf(replacements[i + 1]);
                raw = raw.replace("{" + target + "}", replacement);
                raw = raw.replace("%" + target + "%", replacement);
            }
        }

        return TextUtil.colorize(raw);
    }

    public String getMessage(UUID playerUuid, MessageKey key, Object... replacements) {
        return getMessage(playerUuid, key.getPath(), replacements);
    }

    public String getMessage(UUID playerUuid, String path, Object... replacements) {
        String lang = getPlayerLanguage(playerUuid);
        return getMessage(lang, path, replacements);
    }

    public List<String> getMessageList(Player player, String path) {
        String lang = getPlayerLanguage(player);
        String code = normalize(lang);

        FileConfiguration config = languageConfigs.get(code);
        if (config == null || !config.contains(path)) {
            config = languageConfigs.get(defaultLanguage);
        }
        if (config == null || !config.contains(path)) {
            config = languageConfigs.get("en");
        }

        List<String> list = new ArrayList<>();
        if (config != null && config.contains(path)) {
            List<String> rawList = config.getStringList(path);
            for (String line : rawList) {
                list.add(TextUtil.colorize(line));
            }
        }
        return list;
    }

    public void sendMessage(CommandSender sender, MessageKey key, Object... replacements) {
        sendMessage(sender, key.getPath(), replacements);
    }

    public void sendMessage(CommandSender sender, String path, Object... replacements) {
        if (sender == null) return;
        String message = getMessage(sender, path, replacements);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendMessage(Player player, MessageKey key, Object... replacements) {
        sendMessage((CommandSender) player, key, replacements);
    }

    public void sendMessage(Player player, String path, Object... replacements) {
        sendMessage((CommandSender) player, path, replacements);
    }

    public boolean isLanguageLoaded(String langCode) {
        return languageConfigs.containsKey(normalize(langCode));
    }

    public List<String> getAvailableLanguages() {
        return new ArrayList<>(languageConfigs.keySet());
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public FileConfiguration getLanguageConfig(String langCode) {
        return languageConfigs.get(normalize(langCode));
    }

    public void registerLanguage(String code, FileConfiguration config) {
        if (code != null && config != null) {
            languageConfigs.put(normalize(code), config);
        }
    }

    public void setDefaultLanguage(String lang) {
        this.defaultLanguage = normalize(lang);
    }
}