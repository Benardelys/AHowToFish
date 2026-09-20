package com.ardelys.ahowtofish.command;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.competition.CompetitionType;
import com.ardelys.ahowtofish.model.Fish;
import com.ardelys.ahowtofish.model.FishingBait;
import com.ardelys.ahowtofish.model.FishingRod;
import com.ardelys.ahowtofish.model.FishingZone;
import com.ardelys.ahowtofish.model.SpecialEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class FishingTabCompleter implements TabCompleter {
    private final AHowToFishPlugin plugin;

    public FishingTabCompleter(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> list = new ArrayList<>(Arrays.asList(
                    "help", "menu", "hub", "status", "stats", "collection", "quests", "quest", "achievements",
                    "leaderboard", "shop", "sell", "sellall", "language"
            ));
            if (sender.hasPermission("ahowtofish.admin") || sender.hasPermission("ahowtofish.admin.diagnostics")) {
                list.addAll(Arrays.asList(
                        "admin", "reload", "give", "giverod", "givebait", "setlevel", "setxp",
                        "addxp", "reset", "competition", "event", "zone", "npc", "starter", "diagnostics",
                        "performance", "security", "securitycheck"
                ));
            }
            return filter(list, args[0]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "give", "giverod", "givebait", "setlevel", "setxp", "addxp", "reset" -> {
                    if (sender.hasPermission("ahowtofish.admin")) {
                        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[1]);
                    }
                }
                case "starter" -> {
                    if (sender.hasPermission("ahowtofish.admin.starter.reset") || sender.hasPermission("ahowtofish.admin")) {
                        return filter(Collections.singletonList("reset"), args[1]);
                    }
                }
                case "quest" -> {
                    List<String> qOpts = new ArrayList<>(Arrays.asList("list", "info"));
                    if (sender.hasPermission("ahowtofish.admin.quest") || sender.hasPermission("ahowtofish.admin")) {
                        qOpts.addAll(Arrays.asList("start", "complete", "skip", "reset"));
                    }
                    if (plugin.getQuestManager() != null) {
                        qOpts.addAll(plugin.getQuestManager().getAllQuests().stream().map(q -> q.id()).collect(Collectors.toList()));
                    }
                    return filter(qOpts, args[1]);
                }
                case "competition" -> {
                    if (sender.hasPermission("ahowtofish.admin.competition") || sender.hasPermission("ahowtofish.admin")) {
                        return filter(Arrays.asList("start", "stop", "info"), args[1]);
                    }
                }
                case "event" -> {
                    if (sender.hasPermission("ahowtofish.admin.event") || sender.hasPermission("ahowtofish.admin")) {
                        return filter(Arrays.asList("start", "stop"), args[1]);
                    }
                }
                case "zone" -> {
                    if (sender.hasPermission("ahowtofish.admin.zone") || sender.hasPermission("ahowtofish.admin")) {
                        return filter(Arrays.asList("create", "delete", "setpos1", "setpos2", "info", "reload"), args[1]);
                    }
                }
                case "npc" -> {
                    if (sender.hasPermission("ahowtofish.admin.npc") || sender.hasPermission("ahowtofish.admin")) {
                        return filter(Arrays.asList("set", "remove", "teleport", "reload", "info"), args[1]);
                    }
                }
                case "language" -> {
                    List<String> langOptions = new ArrayList<>(plugin.getLanguageManager().getAvailableLanguages());
                    langOptions.add("default");
                    if (sender.hasPermission("ahowtofish.admin.diagnostics") || sender.hasPermission("ahowtofish.admin.language") || sender.hasPermission("ahowtofish.admin")) {
                        langOptions.add("info");
                    }
                    return filter(langOptions, args[1]);
                }
            }
        }

        if (args.length == 3) {
            switch (sub) {
                case "starter" -> {
                    if (args[1].equalsIgnoreCase("reset") && (sender.hasPermission("ahowtofish.admin.starter.reset") || sender.hasPermission("ahowtofish.admin"))) {
                        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
                    }
                }
                case "quest" -> {
                    String action = args[1].toLowerCase(Locale.ROOT);
                    if (action.equals("info") && plugin.getQuestManager() != null) {
                        return filter(plugin.getQuestManager().getAllQuests().stream().map(q -> q.id()).collect(Collectors.toList()), args[2]);
                    }
                    if ((action.equals("start") || action.equals("complete") || action.equals("skip") || action.equals("reset")) &&
                            (sender.hasPermission("ahowtofish.admin.quest") || sender.hasPermission("ahowtofish.admin"))) {
                        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
                    }
                }
                case "give" -> {
                    if (sender.hasPermission("ahowtofish.admin")) {
                        return filter(plugin.getFishManager().getAllFish().stream().map(Fish::id).collect(Collectors.toList()), args[2]);
                    }
                }
                case "giverod" -> {
                    if (sender.hasPermission("ahowtofish.admin")) {
                        return filter(plugin.getRodManager().getAllRods().stream().map(FishingRod::id).collect(Collectors.toList()), args[2]);
                    }
                }
                case "givebait" -> {
                    if (sender.hasPermission("ahowtofish.admin")) {
                        return filter(plugin.getBaitManager().getAllBaits().stream().map(FishingBait::id).collect(Collectors.toList()), args[2]);
                    }
                }
                case "competition" -> {
                    if (args[1].equalsIgnoreCase("start")) {
                        return filter(Arrays.stream(CompetitionType.values()).map(Enum::name).collect(Collectors.toList()), args[2]);
                    }
                }
                case "event" -> {
                    if (args[1].equalsIgnoreCase("start")) {
                        return filter(plugin.getEventManager().getAllEvents().stream().map(SpecialEvent::id).collect(Collectors.toList()), args[2]);
                    }
                }
                case "zone" -> {
                    if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("info")) {
                        return filter(plugin.getZoneManager().getAllZones().stream().map(FishingZone::id).collect(Collectors.toList()), args[2]);
                    }
                }
            }
        }

        if (args.length == 4) {
            if (sub.equals("quest") && (sender.hasPermission("ahowtofish.admin.quest") || sender.hasPermission("ahowtofish.admin"))) {
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("start") || action.equals("complete") || action.equals("reset")) {
                    List<String> list = new ArrayList<>();
                    if (action.equals("reset")) list.add("all");
                    if (plugin.getQuestManager() != null) {
                        list.addAll(plugin.getQuestManager().getAllQuests().stream().map(q -> q.id()).collect(Collectors.toList()));
                    }
                    return filter(list, args[3]);
                }
            }
        }

        return completions;
    }

    private List<String> filter(List<String> list, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return list.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).collect(Collectors.toList());
    }
}
