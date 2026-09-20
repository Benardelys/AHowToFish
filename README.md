# AHowToFish 🎣
**Production-Ready Advanced Fishing & Fishing Progression Plugin for Paper (1.21.1+ | Java 21)**
*Author: Ardelys*

---

## 🌟 Overview
**AHowToFish** is a modern, modular, production-grade Minecraft Paper plugin that completely revitalizes vanilla fishing into an expansive RPG progression system. It features infinite custom fish, dynamic weight and size calculations, custom fishing rods, bait systems, tiered rarities, interactive GUIs, persistent database storage (SQLite / MySQL / MariaDB), live fishing tournaments, global server events, quest contracts, milestone achievements, permanent skill upgrades, multi-language localization (English & Turkish), and developer APIs.

---

## 📦 Requirements & Compatibility
* **Server Platform:** Paper, Purpur, or compatible forks (1.21.1 and 1.21.x+)
* **Java:** Java 21 LTS (or newer)
* **Soft Dependencies:**
  * **Vault** (Optional, for economy transactions)
  * **PlaceholderAPI** (Optional, for scoreboards & placeholders)
  * **WorldGuard** (Optional, for region protection checks)

---

## 🚀 Installation
1. Download the compiled `AHowToFish-1.0.0.jar` from the `target/` build directory.
2. Place the jar file into your server's `plugins/` directory.
3. (Recommended) Ensure **Vault** and a compatible Economy plugin (e.g. EssentialsX) and **PlaceholderAPI** are installed.
4. Start or restart your server to generate default configurations and database tables.
5. Customize `config.yml`, `fish.yml`, `rods.yml`, `bait.yml`, `zones.yml`, etc. to your preferences.

---

## 🗄️ Database Setup
AHowToFish features an asynchronous database abstraction layer with connection pooling.
Configure `plugins/AHowToFish/database.yml`:
* **SQLite (Default):** Zero setup required. Automatically creates `plugins/AHowToFish/data.db`.
* **MySQL / MariaDB:**
  ```yaml
  database:
    type: "MYSQL" # or "MARIADB"
    mysql:
      host: "localhost"
      port: 3306
      database: "ahowtofish"
      username: "your_user"
      password: "your_password"
      useSSL: false
      pool-size: 10
  ```
All player queries (level, XP, stats, collection, quests, achievements, upgrades) run asynchronously with dirty-state caching and atomic transactions. Zero blocking queries occur on the main Minecraft server tick loop.

---

## 📜 Commands & Tab Completion
*Aliasing:* `/ahowtofish`, `/fishing`, `/fish`

### Player Commands
| Command | Permission | Description |
|---|---|---|
| `/ahowtofish` or `/fishing` | `ahowtofish.menu` | Opens the interactive Fishing Hub GUI |
| `/ahowtofish stats` | `ahowtofish.stats` | View lifetime career statistics |
| `/ahowtofish collection` | `ahowtofish.collection` | Opens the fish discovery encyclopedia |
| `/ahowtofish quests` | `ahowtofish.quests` | View daily, weekly, and permanent quests |
| `/ahowtofish achievements` | `ahowtofish.achievements` | Inspect milestone achievements |
| `/ahowtofish leaderboard` | `ahowtofish.leaderboard` | Inspect top ranking players |
| `/ahowtofish shop` | `ahowtofish.shop` | Access fish market & supplies store |
| `/ahowtofish sell` | `ahowtofish.sell` | Sell the caught fish held in your hand |
| `/ahowtofish sellall` | `ahowtofish.sellall` | Sell all caught fish in your inventory |
| `/ahowtofish language [code]` | `ahowtofish.use` | Change personal language (`en` or `tr`) |

### Administrator Commands
| Command | Permission | Description |
|---|---|---|
| `/ahowtofish reload` | `ahowtofish.admin.reload` | Hot-reloads all configs and languages |
| `/ahowtofish give <player> <fish> [amount]` | `ahowtofish.admin.give` | Gives custom stamped fish |
| `/ahowtofish giverod <player> <rod>` | `ahowtofish.admin.give` | Gives custom fishing rod |
| `/ahowtofish givebait <player> <bait> [amount]` | `ahowtofish.admin.give` | Gives custom bait item |
| `/ahowtofish setlevel <player> <level>` | `ahowtofish.admin.setlevel` | Sets player fishing level |
| `/ahowtofish setxp <player> <xp>` | `ahowtofish.admin.setxp` | Sets player fishing XP |
| `/ahowtofish addxp <player> <xp>` | `ahowtofish.admin.setxp` | Adds fishing XP to player |
| `/ahowtofish reset <player>` | `ahowtofish.admin.reset` | Resets a player's profile |
| `/ahowtofish competition start <type> [secs]` | `ahowtofish.admin.competition` | Starts a live tournament |
| `/ahowtofish competition stop` | `ahowtofish.admin.competition` | Stops active tournament |
| `/ahowtofish event start <id>` | `ahowtofish.admin.event` | Starts timed server event |
| `/ahowtofish event stop` | `ahowtofish.admin.event` | Stops active event |
| `/ahowtofish zone setpos1 / setpos2` | `ahowtofish.admin.zone` | Sets region selection bounds |
| `/ahowtofish zone create <id> [level]` | `ahowtofish.admin.zone` | Creates new cuboid fishing zone |
| `/ahowtofish zone delete <id>` | `ahowtofish.admin.zone` | Deletes a fishing zone |
| `/ahowtofish npc set` | `ahowtofish.admin.npc` | Spawns Fisherman Lucas NPC at location |
| `/ahowtofish npc remove` | `ahowtofish.admin.npc` | Despawns Fisherman Lucas NPC |
| `/ahowtofish npc tp` | `ahowtofish.admin.npc` | Teleports to Fisherman Lucas NPC |

---

## 🔒 Permissions
* `ahowtofish.use` - Basic access to all standard player commands
* `ahowtofish.menu` - Open the GUI main menu
* `ahowtofish.shop` - Access the marketplace
* `ahowtofish.sell` - Sell single fish
* `ahowtofish.sellall` - Sell all inventory fish
* `ahowtofish.admin` - Wildcard permission granting all administrative subcommands
* Multipliers:
  * `ahowtofish.multiplier.x1`
  * `ahowtofish.multiplier.x2`
  * `ahowtofish.multiplier.x3`
  * `ahowtofish.multiplier.x5`

---

## 🧩 PlaceholderAPI Expansion
Expansion identifier: `ahowtofish`

| Placeholder | Description |
|---|---|
| `%ahowtofish_level%` | Player's current fishing level |
| `%ahowtofish_xp%` | Player's current experience points |
| `%ahowtofish_xp_required%` | XP required for next level |
| `%ahowtofish_xp_progress%` | Percentage progress to next level |
| `%ahowtofish_fish_caught%` | Total lifetime catches |
| `%ahowtofish_total_weight%` | Total lifetime weight caught (kg) |
| `%ahowtofish_largest_fish%` | Weight of player's largest fish caught |
| `%ahowtofish_largest_fish_weight%` | Numeric weight of largest fish |
| `%ahowtofish_money_earned%` | Total currency earned from catches |
| `%ahowtofish_rare_fish%` | Number of rare fish caught |
| `%ahowtofish_legendary_fish%` | Number of legendary fish caught |
| `%ahowtofish_current_zone%` | Display name of player's current zone |
| `%ahowtofish_fishing_streak%` | Current successful catch streak |
| `%ahowtofish_best_streak%` | Lifetime best catch streak |
| `%ahowtofish_competition_rank%` | Current rank in active tournament |
| `%ahowtofish_top_<1-10>_name%` | Player name at leaderboard rank |
| `%ahowtofish_top_<1-10>_value%` | Score/Level at leaderboard rank |

---

## 🐟 Creating Custom Fish
Add new entries into `plugins/AHowToFish/fish.yml`:
```yaml
fish:
  golden_koi:
    display-name: "Golden Koi"
    material: GOLD_NUGGET
    custom-model-data: 1001 # Optional custom model data
    rarity: LEGENDARY       # Matches rarities.yml
    weight:
      min: 8.0              # Minimum weight in kg
      max: 35.0             # Maximum weight in kg
    sell-price: 4500.0      # Base sell price
    xp: 2500                # Base XP awarded
    min-level: 45           # Required player level
    chance: 1.0             # Relative encounter weight
    required-zone: ""       # Optional required zone ID
    required-bait: ""       # Optional required bait ID
    lore:
      - "&7A mythical fish said to bring boundless fortune."
```

---

## 🎣 Custom Rods & Baits
### Custom Rods (`rods.yml`)
Configure rods with unique durability, custom model data, and multipliers for luck, money, XP, rare chances, and double-catch:
```yaml
rods:
  diamond_rod:
    display-name: "&bDiamond Abyssal Rod"
    material: FISHING_ROD
    durability: 800
    luck: 10.0
    xp-multiplier: 2.0
    money-multiplier: 1.8
    rare-chance: 20.0
    legendary-chance: 8.0
    double-catch-chance: 10.0
    required-level: 50
```

### Custom Baits (`bait.yml`)
Place bait in the off-hand to consume uses and apply multipliers:
```yaml
baits:
  lucky_bait:
    display-name: "&bFour-Leaf Clover Chum"
    material: FERN
    max-uses: 25
    cost: 750.0
    fish-chance-multiplier: 1.4
    rare-chance-multiplier: 2.0
    legendary-chance-multiplier: 1.8
    xp-multiplier: 1.5
    money-multiplier: 1.3
```

---

## 🏆 Competitions & Events
### Tournaments
Admins can start live tournaments with `/ahowtofish competition start <type> <seconds>`.
Supported types:
* `MOST_FISH`
* `LARGEST_FISH`
* `TOTAL_WEIGHT`
* `HIGHEST_VALUE`
* `MOST_RARE`
Features live Adventure BossBar progress, countdown timers, standings broadcast, and automated prize distribution.

### Timed Server Events
Scheduled automatically or triggered with `/ahowtofish event start <id>`.
* `double_xp` - Wisdom of the Waves (2x XP)
* `double_money` - Golden Tide (2x Sell Prices)
* `rare_frenzy` - Deepsea Frenzy (3x Rare Rates)

---

## 💻 Developer API
Access the singleton instance via:
```java
AHowToFishAPI api = AHowToFishPlugin.getInstance();

// Player queries
int level = api.getFishingLevel(player.getUniqueId());
long xp = api.getFishingXp(player.getUniqueId());
FishingZone zone = api.getCurrentZone(player);

// Programmatic catch rewards
api.giveFish(player, "golden_koi", 1);
api.giveRod(player, "diamond_rod");
```

### Paper Events
* `FishingCatchEvent` (Cancellable)
* `FishingLevelUpEvent`
* `FishDiscoverEvent`
* `FishingCompetitionStartEvent`
* `FishingCompetitionEndEvent`
* `FishingEventStartEvent`
* `FishingEventEndEvent`

---

## 🛠️ Troubleshooting
* **Vault / Economy not working:** Ensure you have Vault installed alongside an economy provider like EssentialsX. If absent, the plugin functions normally using internal leveling and direct XP rewards.
* **Fish not biting:** Check that the player meets the `min-level` of the fish, and that any required bait or zone matches.
* **Character encoding:** All configuration and language files are standard UTF-8.