# 🍌 Pishang Extensions

**Pishang Extensions** is a custom Minecraft server plugin developed to enhance the gameplay experience and provide additional utilities for the Pishang community server.

Built on top of the **VaultAPI** and **EssentialsX** ecosystem, this plugin introduces custom teleportation features, economy integration, and convenient commands for streamers and players.

## 📦 Dependencies

The following plugins are required for Pishang Extensions to function properly:

- **[Vault](https://github.com/MilkBowl/VaultAPI)** – Economy integration.
- **[LuckPerms](https://luckperms.net/)** – Permission management.
- **[EssentialsX](https://github.com/EssentialsX/Essentials)** – Teleportation functionality.

## ✨ Features

### 💰 TPA Charge System
A modified TPA request and acceptance system with economy integration.

- Charges players after a successful teleportation session.
- Supports both `/tpa` and `/tpahere`.
- No charge is applied if the teleport request is denied.
- Integrates with Vault-supported economy plugins.

### 🎥 Streaming & Recording Utilities
Convenient commands for managing player status while streaming or recording.

- `/streaming` – Toggle your streaming status.
- `/recording` – Toggle your recording status.

### 🚀 Custom Teleportation Commands

| Command | Description |
|---|---|
| `/tpa <player>` | Request to teleport to another player. |
| `/tpahere <player>` | Request another player to teleport to you. |
| `/tpacancel` | Cancel your pending teleport request. |

### ⚙️ Plugin Management

| Command | Description |
|---|---|
| `/pishangext info` | Display plugin information. |
| `/pishangext reload` | Reload plugin configurations. |

## 🛠️ Configuration

Pishang Extensions provides a `config.yml` file that allows server administrators to customize plugin behaviour.

Configuration options include:
- TPA charge settings.
- Economy-related settings.
- Custom feature configurations.

Changes can be applied using `/pishangext reload` without manually restarting the server (where supported).

## 📋 Compatibility

- **Server Software:** Paper
- **Dependencies:** Vault, LuckPerms, EssentialsX
- **Language:** Java

## 📜 License

See the [LICENSE](LICENSE) file for more information.
