package com.ardelys.ahowtofish.gui.menus;

import com.ardelys.ahowtofish.AHowToFishPlugin;
import com.ardelys.ahowtofish.gui.CustomGui;
import com.ardelys.ahowtofish.language.LanguageManager;
import com.ardelys.ahowtofish.language.MessageKey;
import com.ardelys.ahowtofish.player.FishingProfile;
import com.ardelys.ahowtofish.utility.ItemBuilder;
import com.ardelys.ahowtofish.utility.SoundParticleUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;

public class LanguageMenu extends CustomGui {
    private final AHowToFishPlugin plugin;
    private final Player player;
    private final FishingProfile profile;
    private final LanguageManager lm;

    public LanguageMenu(AHowToFishPlugin plugin, Player player) {
        super(27, plugin.getLanguageManager().getMessage(player, "gui.language.title", "&1&l✦ Select Language &8| &9Dil Seçimi"), "LANGUAGE_MENU");
        this.plugin = plugin;
        this.player = player;
        this.profile = plugin.getProfileManager().getProfile(player);
        this.lm = plugin.getLanguageManager();

        initialize();
    }

    private void initialize() {
        fillBorder(Material.BLACK_STAINED_GLASS_PANE);

        String activeLang = lm.getPlayerLanguage(player);
        boolean hasExplicit = profile != null && profile.hasExplicitLanguage();
        boolean isEn = hasExplicit && "en".equalsIgnoreCase(profile.getLanguage());
        boolean isTr = hasExplicit && "tr".equalsIgnoreCase(profile.getLanguage());
        boolean isAuto = !hasExplicit;

        String activeText = lm.getMessage(player, "gui.common.currently_active", "&a✔ Currently Active");
        String selectText = lm.getMessage(player, "gui.common.click_to_select", "&eClick to select!");
        String serverDefaultText = lm.getMessage(player, "gui.common.server_default", "&7Server Default: &f{lang}").replace("{lang}", lm.getDefaultLanguage().toUpperCase(Locale.ROOT));

        setItem(10, ItemBuilder.from(Material.BLUE_BANNER)
                .name(lm.getMessage(player, "gui.language.en_name", "&9&lEnglish"))
                .lore(
                        lm.getMessage(player, "gui.language.en_desc", "&7Select English as your personal language."),
                        "",
                        isEn ? activeText : selectText
                )
                .glow(isEn)
                .build(), e -> selectLanguage("en"));

        setItem(12, ItemBuilder.from(Material.RED_BANNER)
                .name(lm.getMessage(player, "gui.language.tr_name", "&c&lTürkçe"))
                .lore(
                        lm.getMessage(player, "gui.language.tr_desc", "&7Kişisel dilinizi Türkçe olarak ayarlayın."),
                        "",
                        isTr ? activeText : selectText
                )
                .glow(isTr)
                .build(), e -> selectLanguage("tr"));

        setItem(14, ItemBuilder.from(Material.COMPASS)
                .name(lm.getMessage(player, "gui.language.auto_name", "&e&lServer Default / Otomatik"))
                .lore(
                        lm.getMessage(player, "gui.language.auto_desc", "&7Follow server default language ({lang}).").replace("{lang}", lm.getDefaultLanguage().toUpperCase(Locale.ROOT)),
                        serverDefaultText,
                        "",
                        isAuto ? activeText : selectText
                )
                .glow(isAuto)
                .build(), e -> selectLanguage("default"));

        setItem(22, ItemBuilder.from(Material.DARK_OAK_DOOR)
                .name(lm.getMessage(player, "gui.common.back", "&cBack to Main Menu"))
                .build(), e -> new MainFishingMenu(plugin, player).open(player));
    }

    private void selectLanguage(String lang) {
        if (profile != null) {
            profile.setLanguage(lang);
            plugin.getProfileManager().saveProfileAsync(profile);
        }
        SoundParticleUtil.playSound(player, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.5f);
        String resolved = lm.getPlayerLanguage(player).toUpperCase(Locale.ROOT);
        lm.sendMessage(player, MessageKey.LANGUAGE_CHANGED, "lang", resolved);
        
        new LanguageMenu(plugin, player).open(player);
    }
}