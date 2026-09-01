# EasyEnchants

EasyEnchants is a Paper plugin that makes enchanting less tedious while preserving normal enchantment compatibility rules.

## Features

### Drag-and-drop enchanted books

Apply an enchanted book by picking it up and clicking it onto an item in your inventory. EasyEnchants applies every compatible enchantment from the book, skips conflicting or lower-level enchantments, and consumes one book when at least one enchantment is applied.

This bypasses the anvil interface and its level cost, but does not allow incompatible enchantments or levels above an enchantment's normal maximum.

### Librarian book selection

A player with the **Luck** effect can right-click an unlocked librarian to open a searchable menu of tradeable enchanted books and levels. Choosing a book guarantees it as the librarian's enchanted-book trade and consumes the player's Luck effect.

A librarian is considered unlocked when it:

- Has the librarian profession
- Has no villager experience
- Has no previously used trades

The emerald price is still randomized using the selected enchantment level, and treasure enchantments cost more.

### In-game settings

Administrators can run `/easyenchants` (or `/ee`) to open a menu that enables or disables each feature. Changes are saved immediately to `config.yml`.

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

Both features are enabled by default:

```yaml
branches:
  drag-and-drop-books:
    enabled: true
  librarian-rolling:
    enabled: true
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
