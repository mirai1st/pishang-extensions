package com.mirai.pishangextensions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class Main extends JavaPlugin {
    public Economy econ;
    public Permission perms;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("No economy provider found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        new TpaCharge(this).register();

        setupPermissions();
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

    private void setupPermissions() {
        RegisteredServiceProvider<Permission> rsp =
                getServer().getServicesManager().getRegistration(Permission.class);

        if (rsp == null) {
            getLogger().warning("No permission provider found.");
            return;
        }

        perms = rsp.getProvider();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // check nama command
        if (!command.getName().equalsIgnoreCase("pishangextension")) return false;

        // kalau tak tulis subcommand
        if (args.length == 0) {
            sender.sendMessage("Usage: /pishangextension <info|reload>");
            return true;  // penting return true
        }

        // handle subcommands
        switch (args[0].toLowerCase()) {
            case "info" -> {
                sender.sendMessage("§aPishangExtension Plugin v" + getDescription().getVersion());
                sender.sendMessage("§aAuthor: Mirai1st");
                sender.sendMessage("§aGithub: https://github.com/mirai1st");
            }
            case "reload" -> {
                reloadConfig();
                sender.sendMessage("§aPishangExtension config reloaded!");
            }
            default -> sender.sendMessage("Unknown subcommand: " + args[0]);
        }

        return true;  // penting supaya command tak “bubble” ke default handler
    }


    public Economy getEconomy() {
        return econ;
    }
}

