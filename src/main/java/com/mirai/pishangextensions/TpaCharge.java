package com.mirai.pishangextensions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class TpaCharge {

    // New request object
    public static class TpaRequest {
        private final UUID requester;
        private final boolean here; // true = tpahere, false = tpa

        public TpaRequest(UUID requester, boolean here) {
            this.requester = requester;
            this.here = here;
        }

        public UUID getRequester() {
            return requester;
        }

        public boolean isHere() {
            return here;
        }
    }

    private final Map<UUID, List<TpaRequest>> pending = new HashMap<>();
    private final Main plugin;

    public TpaCharge(Main plugin) {
        this.plugin = plugin;
    }

    public void register() {

        // ==================== /tpa ====================
        plugin.getCommand("tpa").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player requester)) return true;

            if (args.length != 1) {
                requester.sendMessage("Usage: /tpa <player>");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                requester.sendMessage("Player not found!");
                return true;
            }

            if (!plugin.getConfig().getBoolean("allow-tpa-to-yourself", false)) {
                if (requester == target) {
                    requester.sendMessage("You cannot teleport to yourself.");
                    return true;
                }
            }

            List<TpaRequest> list = pending.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());

            for (TpaRequest req : list) {
                if (req.getRequester().equals(requester.getUniqueId())) {
                    requester.sendMessage("You already sent a TPA request.");
                    return true;
                }
            }

            list.add(new TpaRequest(requester.getUniqueId(), false));

            target.sendMessage(requester.getName() + " wants to teleport to you. Type /tpaccept");

            if (plugin.getConfig().getBoolean("enable-sound-tpa", false)) {
                target.playSound(target.getLocation(), getSound(), 1f, 1f);
            }

            requester.sendMessage("TPA request sent to " + target.getName());

            return true;
        });

        // ==================== /tpahere ====================
        plugin.getCommand("tpahere").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player requester)) return true;

            if (args.length != 1) {
                requester.sendMessage("Usage: /tpahere <player>");
                return true;
            }

            Player target = plugin.getServer().getPlayer(args[0]);
            if (target == null) {
                requester.sendMessage("Player not found!");
                return true;
            }

            if (!plugin.getConfig().getBoolean("allow-tpa-to-yourself", false)) {
                if (requester == target) {
                    requester.sendMessage("You cannot teleport yourself.");
                    return true;
                }
            }

            List<TpaRequest> list = pending.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());

            for (TpaRequest req : list) {
                if (req.getRequester().equals(requester.getUniqueId())) {
                    requester.sendMessage("You already sent a request.");
                    return true;
                }
            }

            list.add(new TpaRequest(requester.getUniqueId(), true));

            target.sendMessage(requester.getName() + " wants you to teleport to them. Type /tpaccept");

            if (plugin.getConfig().getBoolean("enable-sound-tpa", false)) {
                target.playSound(target.getLocation(), getSound(), 1f, 1f);
            }

            requester.sendMessage("TPAHere request sent to " + target.getName());

            return true;
        });

        // ==================== /tpaccept ====================
        plugin.getCommand("tpaccept").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player target)) return true;

            List<TpaRequest> requests = pending.get(target.getUniqueId());

            if (requests == null || requests.isEmpty()) {
                target.sendMessage("No TPA request found.");
                return true;
            }

            TpaRequest req = requests.remove(0);

            if (requests.isEmpty()) {
                pending.remove(target.getUniqueId());
            }

            Player requester = plugin.getServer().getPlayer(req.getRequester());

            if (requester == null) {
                target.sendMessage("Requester is offline.");
                return true;
            }

            processTeleport(requester, target, req.isHere());
            return true;
        });

        // ==================== /tpacceptall ====================
        plugin.getCommand("tpacceptall").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player target)) return true;

            List<TpaRequest> requests = pending.remove(target.getUniqueId());

            if (requests == null || requests.isEmpty()) {
                target.sendMessage("No TPA requests found.");
                return true;
            }

            int count = 0;

            for (TpaRequest req : requests) {
                Player requester = plugin.getServer().getPlayer(req.getRequester());
                if (requester == null) continue;

                processTeleport(requester, target, req.isHere());
                count++;
            }

            target.sendMessage("Accepted " + count + " TPA requests.");
            return true;
        });
    }

    // ==================== TELEPORT LOGIC ====================
    private void processTeleport(Player requester, Player target, boolean isHere) {
        double cost = plugin.getConfig().getDouble("tpa.def-cost", 10.0);
        double costhere = plugin.getConfig().getDouble("tpahere.def-cost", 10.0);
        Economy econ = plugin.getEconomy();

        if (econ.getBalance(requester) < cost) {
            requester.sendMessage("You do not have enough money. Cost: $" + cost);
            target.sendMessage(requester.getName() + " does not have enough money.");
            return;
        }

        econ.withdrawPlayer(requester, cost);

        if (isHere) {
            target.teleport(requester);
            requester.sendMessage("§a" + target.getName() + " has teleported to you! Charged $" + costhere);
            target.sendMessage("§aYou teleported to " + requester.getName());
        } else {
            requester.teleport(target);
            requester.sendMessage("§aTeleported to " + target.getName() + "! Charged $" + cost);
            target.sendMessage("§a" + requester.getName() + " has teleported to you!");
        }
    }

    private Sound getSound() {
        try {
            return Sound.valueOf(
                    plugin.getConfig().getString("sound-tpa", "ENTITY_EXPERIENCE_ORB_PICKUP")
            );
        } catch (Exception e) {
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
    }
}