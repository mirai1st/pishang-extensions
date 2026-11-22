package pishangext;

import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Main extends JavaPlugin {

    private Economy econ;
    private LuckPerms luckPerms;
    private WorldRestrictionManager worldRestrictions;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Hard dependencies first. If missing, stop before registering anything.
        if (!setupEconomy()) {
            getLogger().severe("No economy provider found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<LuckPerms> lpProvider =
                getServer().getServicesManager().getRegistration(LuckPerms.class);

        if (lpProvider == null) {
            getLogger().severe("LuckPerms not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        luckPerms = lpProvider.getProvider();
        worldRestrictions = new WorldRestrictionManager(this);

        // TPA commands
        new TpaCharge(this, worldRestrictions).register();

        // /recording and /streaming
        Commands commands = new Commands(luckPerms);
        getCommand("recording").setExecutor(commands);
        getCommand("streaming").setExecutor(commands);

        // /pishangext is handled by onCommand() below
        getCommand("pishangext").setExecutor(this);

        // GitHub update check (can be turned off in config.yml)
        if (getConfig().getBoolean("check-updates", true)) {
            new UpdateChecker(this).start();
        }

        getLogger().info("plugin has been enabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) return false;

        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("pishangext")) return false;

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /pishangext <info|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> {
                sender.sendMessage("§ePishangExtension Plugin v" + getPluginMeta().getVersion());
                sender.sendMessage("§eAuthor: Mirai1st");
                sender.sendMessage("§eGithub: https://github.com/mirai1st/pishang-extensions");
            }
            case "reload" -> {
                if (!sender.hasPermission("pishangext.admin")) {
                    sender.sendMessage("§cYou don't have permission.");
                    return true;
                }

                try {
                    reloadConfig();
                    worldRestrictions.reload();
                    sender.sendMessage("§aPishangExtension config and world restrictions reloaded!");
                } catch (Exception e) {
                    sender.sendMessage("§cReload failed. Check console.");
                    getLogger().severe("Failed to reload: " + e.getMessage());
                }
            }
            default -> sender.sendMessage("§cUnknown subcommand: " + args[0]);
        }

        return true;
    }

    // Tab completion for /pishangext <info|reload>
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("pishangext") || args.length != 1) {
            return List.of();
        }

        List<String> options = new ArrayList<>();
        options.add("info");

        // Only show "reload" to people who can actually use it.
        if (sender.hasPermission("pishangext.admin")) {
            options.add("reload");
        }

        String typed = args[0].toLowerCase();
        options.removeIf(option -> !option.startsWith(typed));

        return options;
    }

    public Economy getEconomy() {
        return econ;
    }
}