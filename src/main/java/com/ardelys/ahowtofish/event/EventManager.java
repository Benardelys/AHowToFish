package com.ardelys.ahowtofish.event;

import com.ardelys.ahowtofish.api.events.FishingEventEndEvent;
import com.ardelys.ahowtofish.api.events.FishingEventStartEvent;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.model.SpecialEvent;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class EventManager {
    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private final Map<String, SpecialEvent> events = new ConcurrentHashMap<>();

    private SpecialEvent activeEvent = null;
    private int remainingSeconds = 0;
    private BukkitTask tickerTask = null;
    private BukkitTask randomSchedulerTask = null;

    private boolean randomEventsEnabled = true;
    private int randomIntervalMinutes = 45;
    private double randomChance = 40.0;

    public EventManager(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    public void load() {
        events.clear();
        stopEvent();

        File file = new File(plugin.getDataFolder(), "events.yml");
        if (!file.exists()) {
            plugin.saveResource("events.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.randomEventsEnabled = config.getBoolean("settings.random-events-enabled", true);
        this.randomIntervalMinutes = config.getInt("settings.interval-minutes", 45);
        this.randomChance = config.getDouble("settings.chance-percent", 40.0);

        ConfigurationSection section = config.getConfigurationSection("events");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    String name = section.getString(key + ".name", key);
                    String desc = section.getString(key + ".description", "");
                    int duration = section.getInt(key + ".duration-seconds", 300);
                    double xpMul = section.getDouble(key + ".xp-multiplier", 1.0);
                    double moneyMul = section.getDouble(key + ".money-multiplier", 1.0);
                    double rareMul = section.getDouble(key + ".rare-multiplier", 1.0);
                    double legMul = section.getDouble(key + ".legendary-multiplier", 1.0);
                    String bStart = section.getString(key + ".broadcast-start", "&6✦ Event Started: &e" + name);
                    String bEnd = section.getString(key + ".broadcast-end", "&c✦ Event Ended: &e" + name);
                    String sStart = section.getString(key + ".sound-start", "UI_TOAST_CHALLENGE_COMPLETE");
                    String sEnd = section.getString(key + ".sound-end", "ENTITY_EXPERIENCE_ORB_PICKUP");
                    String particle = section.getString(key + ".particle", "FIREWORK");

                    SpecialEvent ev = new SpecialEvent(
                            key.toLowerCase(),
                            name,
                            desc,
                            duration,
                            xpMul,
                            moneyMul,
                            rareMul,
                            legMul,
                            bStart,
                            bEnd,
                            sStart,
                            sEnd,
                            particle
                    );
                    events.put(ev.id(), ev);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "[AHowToFish] Failed to load event '" + key + "': " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("[AHowToFish] Loaded " + events.size() + " fishing event(s).");

        setupRandomScheduler();
    }

    private void setupRandomScheduler() {
        if (randomSchedulerTask != null) {
            randomSchedulerTask.cancel();
        }
        if (!randomEventsEnabled || events.isEmpty()) return;

        long intervalTicks = randomIntervalMinutes * 60L * 20L;
        randomSchedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeEvent != null) return;
            if (ThreadLocalRandom.current().nextDouble(0, 100) <= randomChance) {
                List<SpecialEvent> list = new ArrayList<>(events.values());
                SpecialEvent chosen = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                startEvent(chosen.id());
            }
        }, intervalTicks, intervalTicks);
    }

    public boolean startEvent(String eventId) {
        if (activeEvent != null) return false;
        SpecialEvent event = events.get(eventId.toLowerCase());
        if (event == null) return false;

        this.activeEvent = event;
        this.remainingSeconds = event.durationSeconds();

        Bukkit.getPluginManager().callEvent(new FishingEventStartEvent(event));
        Bukkit.broadcast(TextUtil.toComponent(TextUtil.colorize(event.broadcastStart())));

        for (Player p : Bukkit.getOnlinePlayers()) {
            SoundParticleUtil.playSound(p, event.soundStart(), 1.0f, 1.0f);
        }

        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeEvent == null) {
                stopTicker();
                return;
            }
            remainingSeconds--;
            if (remainingSeconds <= 0) {
                endEvent();
            }
        }, 20L, 20L);

        return true;
    }

    public void stopEvent() {
        if (activeEvent != null) {
            activeEvent = null;
            stopTicker();
        }
    }

    private void endEvent() {
        if (activeEvent == null) return;
        SpecialEvent event = activeEvent;
        activeEvent = null;
        stopTicker();

        Bukkit.getPluginManager().callEvent(new FishingEventEndEvent(event));
        Bukkit.broadcast(TextUtil.toComponent(TextUtil.colorize(event.broadcastEnd())));

        for (Player p : Bukkit.getOnlinePlayers()) {
            SoundParticleUtil.playSound(p, event.soundEnd(), 1.0f, 1.0f);
        }
    }

    private void stopTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
    }

    public SpecialEvent getActiveEvent() {
        return activeEvent;
    }

    public boolean isEventActive() {
        return activeEvent != null;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public Collection<SpecialEvent> getAllEvents() {
        return Collections.unmodifiableCollection(events.values());
    }
}
