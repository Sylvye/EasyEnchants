# EasyEnchants

EasyEnchants is a Paper plugin that makes enchanting less tedious while preserving normal enchantment compatibility rules.

## Features

### Drag-and-drop enchanted books

Apply an enchanted book by picking it up and clicking it onto an item in your inventory. EasyEnchants applies every compatible enchantment from the book, combines matching levels into the next level (for example, Sharpness I + Sharpness I = Sharpness II), skips lower-level enchantments, and consumes one book when at least one enchantment is applied. Incompatible enchantments play an error sound.

This bypasses the anvil interface and its level cost, but does not allow incompatible enchantments or levels above an enchantment's normal maximum.

### Guaranteed villager trades

With the **Luck** effect, right-click an adult librarian or a pre-master fletcher to open a searchable trade menu. Each accepted choice consumes your Luck effect once. Rejected choices consume nothing.

**Librarians** can receive four selections, including duplicate books: one book trade each at novice, apprentice, journeyman, and expert. You can select books after trading with the villager, even at master. A selection replaces an existing unselected book first; otherwise it replaces a non-book trade from an unlocked eligible tier, preferring a sale over a trade that buys your items. Earlier guarantees are preserved. If the next slot is still locked, the book appears as soon as that tier unlocks—even if vanilla would have omitted a book.

For example, four Luck effects can reserve Mending, Unbreaking III, Sharpness V, and Efficiency V on a novice. The first book appears immediately; the others appear at apprentice, journeyman, and expert. Selecting a fifth book is rejected.

**Fletchers** below master can reserve one tipped-arrow trade, including Harming II and extended-duration variants. It appears at master even if vanilla would have omitted tipped arrows. Master fletchers cannot accept selections, and a reserved arrow choice cannot be changed.

Villagers visibly have **Luck while any selected trade is pending**. It disappears when the final promised recipe is available; you do not have to buy it. An immediately fulfilled selection never briefly applies Luck. Existing Luck from another source is left alone.

Book prices use vanilla enchantment-level ranges and the double-trade-price tag. Arrow trades cost **2 emeralds + 5 arrows for 5 tipped arrows**. Stock limits, tier experience, and price multipliers follow vanilla; replacing an existing book preserves its uses and pricing adjustments.

Selections survive restarts, chunk unloads, and zombification/cure. Losing or changing profession clears selections and plugin-applied Luck without refunds. Disabling a rolling feature blocks new choices but still fulfills accepted reservations.

This targets standard Java/Paper 1.21.11 trades. Rolling is disabled in worlds using experimental **Villager Trade Rebalance**. Book selection fails closed if the tradeable-enchantment registry cannot be read. Custom trade layouts supplied by other plugins are not supported. Existing pending selections migrate automatically; completed selections from older EasyEnchants versions have no saved identity and are treated as ordinary trades.

### In-game settings

Administrators can run `/easyenchants` (or `/ee`) to open a menu that enables or disables each feature. Changes are saved immediately to `config.yml`.

### Villager acceleration

New baby villagers grow up faster, zombie villagers cure faster, and villagers can breed again sooner. The module is event-driven and only changes timers when breeding or curing begins. Its enable state and all three values can be changed from the in-game settings menu or `config.yml`.

## Requirements

- Paper 1.21.11 or compatible
- Java 21

## Installation

1. Download or build the plugin JAR.
2. Place it in the server's `plugins` directory.
3. Restart the server.

## Commands and permissions

| Command | Description | Permission |
| --- | --- | --- |
| `/easyenchants` | Opens the settings menu | `easyenchants.admin` |
| `/ee` | Alias for `/easyenchants` | `easyenchants.admin` |

`easyenchants.admin` is granted to server operators by default.

## Configuration

All features are enabled by default. Villager growth and curing run at 5× speed, while breeding cooldown is one minute:

```yaml
branches:
  drag-and-drop-books:
    enabled: true
  librarian-rolling:
    enabled: true
  fletcher-rolling:
    enabled: true
  villager-acceleration:
    enabled: true
    growth-speed-multiplier: 5.0
    curing-speed-multiplier: 5.0
    breeding-cooldown-seconds: 60
```

You can edit `plugins/EasyEnchants/config.yml` while the server is stopped, or use the in-game settings menu.

## Building from source

Clone the repository and run the Gradle wrapper:

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

The built JAR is written to `build/libs/`.

Run the test suite with:

```shell
./gradlew test
```

### Real Paper integration checks

Build the optional smoke-test plugin with `./gradlew integrationTestJar`. On an **isolated Paper 1.21.11 test server**, install `EasyEnchants-26.2.jar` and `EasyEnchants-PaperTests-26.2.jar` from `build/libs/`, then start the server. The test plugin spawns villagers, checks real trade generation, pending Luck particle flags, PDC serialization, and zombification/cure, then shuts down the server. Success logs `EASYENCHANTS_INTEGRATION_PASS` and writes `plugins/EasyEnchantsPaperTests/PASS`.

Do not install the test plugin on a gameplay server. Client-side particle rendering still requires an in-game visual check.
