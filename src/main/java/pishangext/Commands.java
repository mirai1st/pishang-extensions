package pishangext;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private final LuckPerms lp;

    public Commands(LuckPerms luckPerms) {
        this.lp = luckPerms;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        User user = lp.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            player.sendMessage("§cYour permission data is not loaded yet. Try again.");
            return true;
        }

        String name = command.getName().toLowerCase();

        switch (name) {
            case "recording" -> toggle(
                    user, player,
                    SuffixNode.builder("&f [♦]", 100).build(),
                    "§a" + player.getName() + " has started their recording session.",
                    "§a" + player.getName() + " has stopped their recording session."
            );
            case "streaming" -> toggle(
                    user, player,
                    SuffixNode.builder("&c [♦]", 100).build(),
                    "§a" + player.getName() + " has started their streaming session!",
                    "§a" + player.getName() + " has stopped their streaming session!"
            );
            default -> {
                return false;
            }
        }

        lp.getUserManager().saveUser(user);
        return true;
    }

    // Add the suffix if missing, remove it if already there.
    private void toggle(User user, Player player, Node node, String startMsg, String stopMsg) {
        if (user.getNodes().contains(node)) {
            user.data().remove(node);
            Bukkit.broadcastMessage(stopMsg);
        } else {
            user.data().add(node);
            Bukkit.broadcastMessage(startMsg);
        }
    }
}
