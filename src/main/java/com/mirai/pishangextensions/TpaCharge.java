package com.mirai.pishangextensions;

import net.milkbowl.vault.economy.Economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpaCharge {
    private final Map<UUID, UUID> pending = new HashMap<>();
    private final Main plugin;

    public TpaCharge(Main plugin) {
        this.plugin = plugin;
        Economy econ = plugin.getEconomy();
    }

    public void register() {
        plugin.getCommand("tpa").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player requester)) return true;

            if (args.length != 1) {
                requester.sendMessage("Usage: /tpa <player>");
                return true;
            }

            org.bukkit.entity.Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                requester.sendMessage("Player not found!");
                return true;
            }

            if (requester == target) {
                target.sendMessage("You cannot teleport to yourself.");
                return true;
            }

            pending.put(target.getUniqueId(), requester.getUniqueId());
            target.sendMessage(requester.getName() + " wants to teleport to you. Type /tpaccept to accept!");
            requester.sendMessage("TPA request sent to " + target.getName());

            return true;
        });

        plugin.getCommand("tpaccept").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player target)) return true;

            java.util.UUID requesterUUID = pending.remove(target.getUniqueId());
            if (requesterUUID == null) {
                target.sendMessage("No TPA request found.");
                return true;
            }

            org.bukkit.entity.Player requester = plugin.getServer().getPlayer(requesterUUID);
            if (requester == null) {
                target.sendMessage("Requester is offline.");
                return true;
            }

            double cost = plugin.getConfig().getDouble("tpa.cost", 10.0);
            Economy econ = plugin.getEconomy();

            if (econ.getBalance(requester) < cost) {
                requester.sendMessage("You do not have enough money for teleport. Cost: $" + cost);
                target.sendMessage(requester.getName() + " does not have enough money.");
                return true;
            }

            econ.withdrawPlayer(requester, cost);
            requester.teleport(target);
            requester.sendMessage("§a" + target.getName() + " has accepted your TPA request! and charged $" + cost);
            target.sendMessage("§a" + requester.getName() + " has teleported to you!");

            return true;
        });
    }
}