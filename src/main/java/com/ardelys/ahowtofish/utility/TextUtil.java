package com.ardelys.ahowtofish.utility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextUtil {
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final int COMPONENT_CACHE_MAX = 512;
    private static final Map<String, Component> COMPONENT_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(COMPONENT_CACHE_MAX + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Component> eldest) {
                    return size() > COMPONENT_CACHE_MAX;
                }
            }
    );

    public static void clearComponentCache() {
        COMPONENT_CACHE.clear();
    }

    public static int getComponentCacheSize() {
        return COMPONENT_CACHE.size();
    }

    private TextUtil() {}

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        if (message.indexOf('&') == -1 && message.indexOf('<') == -1 && message.indexOf('§') == -1) {
            return message;
        }

        if (message.contains("<") && message.contains(">") && !message.contains("<#")) {
            try {
                Component comp = MINI_MESSAGE.deserialize(message);
                return LegacyComponentSerializer.legacySection().serialize(comp);
            } catch (Exception ignored) {
                
            }
        }

        Matcher matcher = HEX_PATTERN_1.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);

        message = sb.toString();
        Matcher matcher2 = HEX_PATTERN_2.matcher(message);
        StringBuilder sb2 = new StringBuilder();
        while (matcher2.find()) {
            String hex = matcher2.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher2.appendReplacement(sb2, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher2.appendTail(sb2);

        return translateAlternateColorCodes('&', sb2.toString());
    }

    public static String translateAlternateColorCodes(char altColorChar, String text) {
        if (text == null) return "";
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static void sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        Component titleComp = (title != null && !title.isEmpty()) ? toComponent(title) : Component.empty();
        Component subtitleComp = (subtitle != null && !subtitle.isEmpty()) ? toComponent(subtitle) : Component.empty();
        Title.Times times = Title.Times.times(
                Duration.ofMillis(Math.max(0, fadeInTicks) * 50L),
                Duration.ofMillis(Math.max(0, stayTicks) * 50L),
                Duration.ofMillis(Math.max(0, fadeOutTicks) * 50L)
        );
        player.showTitle(Title.title(titleComp, subtitleComp, times));
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        player.sendActionBar(toComponent(message));
    }

    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        Component cached = COMPONENT_CACHE.get(message);
        if (cached != null) {
            return cached;
        }

        Component comp;
        if (message.contains("<") && message.contains(">") && !message.contains("<#")) {
            try {
                comp = MINI_MESSAGE.deserialize(message);
                COMPONENT_CACHE.put(message, comp);
                return comp;
            } catch (Exception ignored) {
            }
        }
        comp = LegacyComponentSerializer.legacySection().deserialize(colorize(message));
        COMPONENT_CACHE.put(message, comp);
        return comp;
    }

    public static List<String> colorize(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>(list.size());
        for (String s : list) {
            colored.add(colorize(s));
        }
        return colored;
    }

    public static List<Component> toComponentList(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<Component> components = new ArrayList<>(list.size());
        for (String s : list) {
            components.add(toComponent(s));
        }
        return components;
    }

    public static String formatDecimal(double value) {
        return NUMBER_FORMAT.format(value);
    }

    public static String formatInteger(long value) {
        return INTEGER_FORMAT.format(value);
    }

    public static String formatTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString().trim();
    }

    public static String buildProgressBar(double current, double max, int totalBars, String symbol, String completedColor, String incompleteColor) {
        if (max <= 0) max = 1;
        float percent = (float) Math.min(Math.max(current / max, 0.0), 1.0);
        int progressBars = (int) (totalBars * percent);
        int leftOver = totalBars - progressBars;

        StringBuilder sb = new StringBuilder();
        sb.append(completedColor);
        for (int i = 0; i < progressBars; i++) {
            sb.append(symbol);
        }
        sb.append(incompleteColor);
        for (int i = 0; i < leftOver; i++) {
            sb.append(symbol);
        }
        return colorize(sb.toString());
    }
}
