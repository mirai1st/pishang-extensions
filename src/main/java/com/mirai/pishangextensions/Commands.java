package com.mirai.pishangextensions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getServer;

public class Commands implements CommandExecutor {

    // importing LuckPerms as lp (Private)
    private final LuckPerms lp;

    // Constructor
    public Commands(LuckPerms luckPerms) {
        this.lp = luckPerms;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof org.bukkit.entity.Player requester)) return true;
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        User user = lp.getUserManager().getUser(player.getUniqueId());

        if (command.getName().equalsIgnoreCase("recording")) {
            // If player already recording, remove suffix

            Node recording = Node.builder("suffix.100.&f [♦]").build();

            if (!user.getNodes().contains(recording)) {
                user.data().add(recording);
                getServer().broadcastMessage("§a" + requester.getName() + " has started their recording session.");
            } else {
                user.data().remove(recording);
                getServer().broadcastMessage("§a" + requester.getName() + " has stopped their recording session.");
            }
        }

        if (command.getName().equalsIgnoreCase("streaming")) {
            Node streaming = Node.builder("suffix.100.&c [♦]").build();

            if (!user.getNodes().contains(streaming)) {
                user.data().add(streaming);
                getServer().broadcastMessage("§a" + requester.getName() + " has started their streaming session!");
            } else {
                user.data().remove(streaming);
                getServer().broadcastMessage("§a" + requester.getName() + " has stopped their streaming session!");
            }
        }

        lp.getUserManager().saveUser(user);

        return true;
    }
}