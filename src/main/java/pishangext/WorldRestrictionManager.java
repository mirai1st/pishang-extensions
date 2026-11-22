package pishangext;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class WorldRestrictionManager {

    public enum Action {
        TPA,
        TPAHERE
    }

    private final JavaPlugin plugin;
    private final File file;

    private YamlConfiguration config;

    public WorldRestrictionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "world-restrictions.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }

        if (!file.exists()) {
            plugin.saveResource("world-restrictions.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Returns the configured denial message, or null if allowed.
     * fromWorld = world of the player who moves, toWorld = destination world.
     */
    public String getBlockedMessage(String fromWorld, String toWorld, Action action) {

        // "restrictions" is a YAML list, so read it as a list of maps.
        for (Map<?, ?> rule : config.getMapList("restrictions")) {

            String from = asString(rule.get("from"), "*");
            String to = asString(rule.get("to"), "*");

            if (!matches(from, fromWorld) || !matches(to, toWorld)) {
                continue;
            }

            if (!hasAction(rule.get("actions"), action)) {
                continue;
            }

            String message = asString(
                    rule.get("message"),
                    "&cTeleport between these worlds is restricted."
            );

            return ChatColor.translateAlternateColorCodes('&', message);
        }

        return null;
    }

    private boolean hasAction(Object actions, Action action) {
        if (!(actions instanceof List<?> list)) {
            return false;
        }

        for (Object value : list) {
            if (action.name().equalsIgnoreCase(String.valueOf(value))) {
                return true;
            }
        }

        return false;
    }

    private String asString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private boolean matches(String pattern, String worldName) {
        return "*".equals(pattern) || pattern.equalsIgnoreCase(worldName);
    }
}
