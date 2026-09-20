package com.ardelys.ahowtofish.npc;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.menus.FishermanLucasMenu;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.PdcKeys;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import com.ardelys.ahowtofish.utility.TextUtil;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NpcManager implements Listener {

    private final AHowToFishPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    private boolean enabled = true;
    private String displayName = "&6&lFisherman Lucas";
    private String subtitle = "&7Fish Merchant";
    private String worldName = "world";
    private double locX = 0.5, locY = 64.0, locZ = 0.5;
    private float locYaw = 0.0f, locPitch = 0.0f;
    private String entityTypeStr = "ARMOR_STAND";
    private String skinType = "TEXTURE";
    private String skinValue = "";

    private boolean hologramEnabled = true;
    private List<String> hologramLines = new ArrayList<>();

    private boolean interactionEnabled = true;
    private double interactionRange = 4.0;

    private String dialogueInteract = "&6Fisherman Lucas: &7\"Got some fish for me?\"";
    private String dialogueSell = "&6Fisherman Lucas: &7\"Nice catch! I'll take these.\"";
    private String dialogueSellRare = "&6Fisherman Lucas: &d\"Now that's a beautiful catch!\"";
    private String dialogueEmpty = "&6Fisherman Lucas: &7\"Come back when you've caught something.\"";

    private boolean soundsEnabled = true;
    private String soundInteract = "ENTITY_VILLAGER_YES";
    private String soundSell = "ENTITY_PLAYER_LEVELUP";
    private String soundSellRare = "UI_TOAST_CHALLENGE_COMPLETE";
    private String soundEmpty = "ENTITY_VILLAGER_NO";

    private boolean particlesEnabled = true;
    private String particleType = "WATER_SPLASH";
    private int particleInterval = 80;

    private boolean lookAtPlayers = true;
    private boolean confirmationEnabled = true;
    private double confirmationMinValue = 10000.0;

    private UUID npcEntityUuid = null;
    private UUID hologramEntityUuid = null;
    private BukkitTask animationTask = null;
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();

    public NpcManager(AHowToFishPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        stopTasks();
        removeExistingNpcEntities();

        this.configFile = new File(plugin.getDataFolder(), "npc.yml");
        if (!configFile.exists()) {
            plugin.saveResource("npc.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);

        this.enabled = config.getBoolean("fisherman-lucas.enabled", true);
        this.displayName = config.getString("fisherman-lucas.display-name", "&6&lFisherman Lucas");
        this.subtitle = config.getString("fisherman-lucas.subtitle", "&7Fish Merchant");
        this.worldName = config.getString("fisherman-lucas.world", "world");

        this.locX = config.getDouble("fisherman-lucas.location.x", 0.5);
        this.locY = config.getDouble("fisherman-lucas.location.y", 64.0);
        this.locZ = config.getDouble("fisherman-lucas.location.z", 0.5);
        this.locYaw = (float) config.getDouble("fisherman-lucas.location.yaw", 0.0);
        this.locPitch = (float) config.getDouble("fisherman-lucas.location.pitch", 0.0);

        this.entityTypeStr = config.getString("fisherman-lucas.entity-type", "VILLAGER");
        this.skinType = config.getString("fisherman-lucas.skin.type", "TEXTURE");
        this.skinValue = config.getString("fisherman-lucas.skin.value", "");

        this.hologramEnabled = config.getBoolean("fisherman-lucas.hologram.enabled", true);
        this.hologramLines = config.getStringList("fisherman-lucas.hologram.lines");
        if (hologramLines.isEmpty()) {
            hologramLines = Arrays.asList("&6&lFisherman Lucas", "&7Fish Merchant", "&e[Right-Click to Sell]");
        }

        this.interactionEnabled = config.getBoolean("fisherman-lucas.interaction.enabled", true);
        this.interactionRange = config.getDouble("fisherman-lucas.interaction.range", 4.0);

        this.dialogueInteract = config.getString("fisherman-lucas.dialogue.interact", dialogueInteract);
        this.dialogueSell = config.getString("fisherman-lucas.dialogue.sell", dialogueSell);
        this.dialogueSellRare = config.getString("fisherman-lucas.dialogue.sell-rare", dialogueSellRare);
        this.dialogueEmpty = config.getString("fisherman-lucas.dialogue.empty", dialogueEmpty);

        this.soundsEnabled = config.getBoolean("fisherman-lucas.sounds.enabled", true);
        this.soundInteract = config.getString("fisherman-lucas.sounds.interact", soundInteract);
        this.soundSell = config.getString("fisherman-lucas.sounds.sell", soundSell);
        this.soundSellRare = config.getString("fisherman-lucas.sounds.sell-rare", soundSellRare);
        this.soundEmpty = config.getString("fisherman-lucas.sounds.empty", soundEmpty);

        this.particlesEnabled = config.getBoolean("fisherman-lucas.particles.enabled", true);
        this.particleType = config.getString("fisherman-lucas.particles.type", "WATER_SPLASH");
        this.particleInterval = Math.max(20, config.getInt("fisherman-lucas.particles.interval", 80));

        this.lookAtPlayers = config.getBoolean("fisherman-lucas.animation.look-at-players", true);
        this.confirmationEnabled = config.getBoolean("fisherman-lucas.confirmation.enabled", true);
        this.confirmationMinValue = config.getDouble("fisherman-lucas.confirmation.minimum-value", 10000.0);

        if (enabled) {
            spawnNpc();
            startTasks();
        }
    }

    public void stop() {
        stopTasks();
        removeExistingNpcEntities();
        clickCooldowns.clear();
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            clickCooldowns.remove(uuid);
        }
    }

    private void startTasks() {
        if (animationTask != null) {
            animationTask.cancel();
        }

        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled) return;

            World world = Bukkit.getWorld(worldName);
            if (world == null) return;

            Location npcLoc = getNpcLocation();
            if (npcLoc == null || !npcLoc.isChunkLoaded()) return;

            if (particlesEnabled) {
                Location particleLoc = npcLoc.clone().add(0, 0.2, 0);
                SoundParticleUtil.spawnParticle(particleLoc, particleType, 3, 0.3, 0.1, 0.3, 0.02);
            }

            if (lookAtPlayers && npcEntityUuid != null) {
                Entity npc = world.getEntity(npcEntityUuid);
                if (npc instanceof LivingEntity living) {
                    Player closest = null;
                    double closestDistSq = 36.0;
                    for (Player p : world.getPlayers()) {
                        double d = p.getLocation().distanceSquared(npcLoc);
                        if (d < closestDistSq) {
                            closest = p;
                            closestDistSq = d;
                        }
                    }
                    if (closest != null) {
                        double dx = closest.getLocation().getX() - npcLoc.getX();
                        double dy = closest.getEyeLocation().getY() - (npcLoc.getY() + living.getEyeHeight());
                        double dz = closest.getLocation().getZ() - npcLoc.getZ();
                        double distanceXZ = Math.sqrt(dx * dx + dz * dz);

                        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distanceXZ));

                        if (living instanceof ArmorStand stand) {
                            stand.setHeadPose(new EulerAngle(Math.toRadians(pitch), 0, 0));
                            Location newLoc = stand.getLocation();
                            newLoc.setYaw(yaw);
                            stand.teleport(newLoc);
                        } else {
                            Location newLoc = living.getLocation();
                            newLoc.setYaw(yaw);
                            newLoc.setPitch(pitch);
                            living.teleport(newLoc);
                        }
                    }
                }
            }
        }, 40L, 40L);
    }

    private void stopTasks() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    public void spawnNpc() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("[AHowToFish] Fisherman Lucas could not be spawned. World '" + worldName + "' does not exist.");
            return;
        }

        Location loc = getNpcLocation();
        if (loc == null) return;

        if (!loc.isChunkLoaded()) {
            return; 
        }

        removeExistingNpcEntities();

        try {
            Entity mainEntity;
            if ("ARMOR_STAND".equalsIgnoreCase(entityTypeStr)) {
                ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
                stand.setCustomNameVisible(false);
                stand.setGravity(false);
                stand.setArms(true);
                stand.setBasePlate(false);
                stand.setInvulnerable(true);
                stand.setCollidable(false);
                stand.getPersistentDataContainer().set(PdcKeys.NPC_ID, PersistentDataType.STRING, "fisherman_lucas");

                equipFishermanGear(stand);
                mainEntity = stand;
            } else {
                Villager villager = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
                villager.setProfession(Villager.Profession.FISHERMAN);
                villager.setVillagerType(Villager.Type.PLAINS);
                villager.setVillagerLevel(2);
                villager.setAdult();
                villager.setAgeLock(true);
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.setCollidable(false);
                villager.setCustomNameVisible(false);
                villager.setRemoveWhenFarAway(false);
                villager.setPersistent(true);
                villager.setRecipes(Collections.emptyList());
                villager.getPersistentDataContainer().set(PdcKeys.NPC_ID, PersistentDataType.STRING, "fisherman_lucas");
                mainEntity = villager;
            }

            this.npcEntityUuid = mainEntity.getUniqueId();

            if (hologramEnabled && !hologramLines.isEmpty()) {
                Location holoLoc = loc.clone().add(0, 2.35, 0);
                TextDisplay textDisplay = (TextDisplay) world.spawnEntity(holoLoc, EntityType.TEXT_DISPLAY);
                textDisplay.setBillboard(Display.Billboard.CENTER);
                textDisplay.setDefaultBackground(false);
                textDisplay.getPersistentDataContainer().set(PdcKeys.NPC_ID, PersistentDataType.STRING, "fisherman_lucas");

                String joined = String.join("\n", hologramLines);
                textDisplay.text(TextUtil.toComponent(joined));
                this.hologramEntityUuid = textDisplay.getUniqueId();
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[AHowToFish] Error spawning Fisherman Lucas: " + e.getMessage(), e);
        }
    }

    private void equipFishermanGear(ArmorStand stand) {
        
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (skinValue != null && !skinValue.isBlank()) {
                try {
                    PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), "FishermanLucas");
                    profile.setProperty(new ProfileProperty("textures", skinValue));
                    meta.setPlayerProfile(profile);
                } catch (Exception e) {
                    plugin.getLogger().fine("[AHowToFish] Could not set custom skin texture, using default skull.");
                }
            }
            head.setItemMeta(meta);
        }
        stand.getEquipment().setHelmet(head);

        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chest.getItemMeta();
        if (chestMeta != null) {
            chestMeta.setColor(Color.fromRGB(40, 80, 110));
            chest.setItemMeta(chestMeta);
        }
        stand.getEquipment().setChestplate(chest);

        ItemStack legs = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legsMeta = (LeatherArmorMeta) legs.getItemMeta();
        if (legsMeta != null) {
            legsMeta.setColor(Color.fromRGB(115, 95, 65));
            legs.setItemMeta(legsMeta);
        }
        stand.getEquipment().setLeggings(legs);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.setColor(Color.fromRGB(50, 35, 20));
            boots.setItemMeta(bootsMeta);
        }
        stand.getEquipment().setBoots(boots);

        stand.getEquipment().setItemInMainHand(new ItemStack(Material.FISHING_ROD));
    }

    public void removeExistingNpcEntities() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        for (Entity e : world.getEntities()) {
            if (isLucasEntity(e)) {
                e.remove();
            }
        }

        npcEntityUuid = null;
        hologramEntityUuid = null;
    }

    public boolean isLucasEntity(Entity entity) {
        if (entity == null) return false;
        if (entity.getUniqueId().equals(npcEntityUuid) || entity.getUniqueId().equals(hologramEntityUuid)) {
            return true;
        }
        String tag = entity.getPersistentDataContainer().get(PdcKeys.NPC_ID, PersistentDataType.STRING);
        return "fisherman_lucas".equals(tag);
    }

    public Location getNpcLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, locX, locY, locZ, locYaw, locPitch);
    }

    public void setNpcLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;

        this.worldName = loc.getWorld().getName();
        this.locX = loc.getX();
        this.locY = loc.getY();
        this.locZ = loc.getZ();
        this.locYaw = loc.getYaw();
        this.locPitch = loc.getPitch();

        config.set("fisherman-lucas.world", worldName);
        config.set("fisherman-lucas.location.x", locX);
        config.set("fisherman-lucas.location.y", locY);
        config.set("fisherman-lucas.location.z", locZ);
        config.set("fisherman-lucas.location.yaw", locYaw);
        config.set("fisherman-lucas.location.pitch", locPitch);
        config.set("fisherman-lucas.enabled", true);
        this.enabled = true;

        saveConfigFile();
        spawnNpc();
    }

    public void removeNpc() {
        this.enabled = false;
        config.set("fisherman-lucas.enabled", false);
        saveConfigFile();
        removeExistingNpcEntities();
    }

    private void saveConfigFile() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[AHowToFish] Could not save npc.yml: " + e.getMessage());
        }
    }

    public boolean isNearLucas(Player player, double radius) {
        if (!enabled || player == null || !player.isOnline()) return false;
        Location loc = getNpcLocation();
        if (loc == null || !loc.getWorld().equals(player.getWorld())) return false;
        return loc.distanceSquared(player.getLocation()) <= (radius * radius);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handleNpcInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        handleNpcInteraction(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleNpcInteraction(Player player, Entity clicked, org.bukkit.event.Cancellable event) {
        if (!isLucasEntity(clicked)) return;

        event.setCancelled(true);

        if (!interactionEnabled) return;

        if (plugin.getSecurityManager() != null) {
            var sm = plugin.getSecurityManager();
            if (!sm.getRateLimiter().tryAcquire(player.getUniqueId(), com.ardelys.ahowtofish.security.RateLimitCategory.NPC_INTERACTION)) {
                var resp = sm.handleViolation(
                        com.ardelys.ahowtofish.security.SecurityAlertLevel.LOW,
                        com.ardelys.ahowtofish.security.RateLimitCategory.NPC_INTERACTION,
                        "NpcManager",
                        "Exceeded NPC interaction rate limit",
                        player.getUniqueId()
                );
                if (resp.shouldBlock()) {
                    return;
                }
            }
        }

        long now = System.currentTimeMillis();
        Long last = clickCooldowns.get(player.getUniqueId());
        if (last != null && now - last < 500) {
            return;
        }
        clickCooldowns.put(player.getUniqueId(), now);

        Location npcLoc = getNpcLocation();
        if (npcLoc == null || !player.getWorld().equals(npcLoc.getWorld()) || player.getLocation().distance(npcLoc) > interactionRange) {
            return;
        }

        com.ardelys.ahowtofish.player.FishingProfile profile = plugin.getProfileManager().getProfile(player);
        if (profile != null && plugin.getQuestManager() != null) {
            plugin.getQuestManager().onNpcTalk(player, profile);
        }

        if (soundsEnabled) {
            SoundParticleUtil.playSound(player, soundInteract, 1.0f, 1.0f);
        }

        new com.ardelys.ahowtofish.gui.menus.LucasDialogueMenu(plugin, player).open(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (isLucasEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!enabled) return;
        Location loc = getNpcLocation();
        if (loc == null) return;

        if (event.getWorld().equals(loc.getWorld()) &&
                event.getChunk().getX() == loc.getBlockX() >> 4 &&
                event.getChunk().getZ() == loc.getBlockZ() >> 4) {
            
            if (npcEntityUuid == null || Bukkit.getEntity(npcEntityUuid) == null) {
                spawnNpc();
            }
        }
    }

    public boolean isEnabled() { return enabled; }
    public String getDisplayName() { return displayName; }
    public String getSubtitle() { return subtitle; }
    public String getWorldName() { return worldName; }
    public double getLocX() { return locX; }
    public double getLocY() { return locY; }
    public double getLocZ() { return locZ; }
    public boolean isHologramEnabled() { return hologramEnabled; }
    public boolean isConfirmationEnabled() { return confirmationEnabled; }
    public double getConfirmationMinValue() { return confirmationMinValue; }
    public String getDialogueInteract() { return dialogueInteract; }
    public String getDialogueSell() { return dialogueSell; }
    public String getDialogueSellRare() { return dialogueSellRare; }
    public String getDialogueEmpty() { return dialogueEmpty; }
    public String getSoundSell() { return soundSell; }
    public String getSoundSellRare() { return soundSellRare; }
    public String getSoundEmpty() { return soundEmpty; }
}