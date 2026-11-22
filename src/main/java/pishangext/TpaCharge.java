package pishangext;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class TpaCharge {
    private final Map<UUID, List<TpaRequest>> pending = new HashMap<>();

    private final Main plugin;
    private final WorldRestrictionManager worldRestrictions;

    public static class TpaRequest {

        private final UUID requester;
        private final boolean here;
        private final long createdAt;
        private final long expiryMillis;

        public TpaRequest(
                UUID requester,
                boolean here,
                long expiryMillis
        ) {
            this.requester = requester;
            this.here = here;
            this.createdAt = System.currentTimeMillis();
            this.expiryMillis = expiryMillis;
        }

        public UUID getRequester() {
            return requester;
        }

        public boolean isHere() {
            return here;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt >= expiryMillis;
        }
    }

    public TpaCharge(
            Main plugin,
            WorldRestrictionManager worldRestrictions
    ) {
        this.plugin = plugin;
        this.worldRestrictions = worldRestrictions;
    }

    private long getExpiryMillis() {
        long seconds = Math.max(
                1L,
                plugin.getConfig().getLong("tpa-expiry", 180)
        );

        return seconds * 1000L;
    }

    public void register() {

        // Expire requests every second.
        plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> pending.entrySet().removeIf(entry -> {
                    entry.getValue().removeIf(TpaRequest::isExpired);
                    return entry.getValue().isEmpty();
                }),
                20L,
                20L
        );

        // ==================== /tpa ====================

        plugin.getCommand("tpa").setExecutor(
                (sender, command, label, args) -> {

                    if (!(sender instanceof Player requester)) {
                        return true;
                    }

                    if (args.length != 1) {
                        requester.sendMessage("§eUsage: /tpa <player>");
                        return true;
                    }

                    Player target = plugin.getServer().getPlayer(args[0]);

                    if (target == null) {
                        requester.sendMessage("§cPlayer not found!");
                        return true;
                    }

                    if (!plugin.getConfig().getBoolean(
                            "allow-tpa-to-yourself", false)
                            && requester.equals(target)) {

                        requester.sendMessage(
                                "§cYou cannot teleport to yourself."
                        );
                        return true;
                    }

                    // Check world restriction before creating request.
                    String blocked = getBlockedMessage(
                            requester,
                            target,
                            WorldRestrictionManager.Action.TPA
                    );

                    if (blocked != null) {
                        requester.sendMessage(blocked);
                        return true;
                    }

                    List<TpaRequest> list = pending.computeIfAbsent(
                            target.getUniqueId(),
                            k -> new ArrayList<>()
                    );

                    removeExpired(list);

                    for (TpaRequest req : list) {
                        if (req.getRequester().equals(
                                requester.getUniqueId())) {

                            requester.sendMessage(
                                    "§cYou already sent a TPA request."
                            );
                            return true;
                        }
                    }

                    list.add(new TpaRequest(
                            requester.getUniqueId(),
                            false,
                            getExpiryMillis()
                    ));

                    target.sendMessage(
                            "§b" + requester.getName()
                                    + " wants to teleport to you. "
                                    + "Type /tpaccept"
                    );

                    playTpaSound(target);

                    requester.sendMessage(
                            "§bTPA request sent to " + target.getName()
                    );

                    return true;
                }
        );

        // ==================== /tpahere ====================

        plugin.getCommand("tpahere").setExecutor(
                (sender, command, label, args) -> {

                    if (!(sender instanceof Player requester)) {
                        return true;
                    }

                    if (args.length != 1) {
                        requester.sendMessage(
                                "§eUsage: /tpahere <player>"
                        );
                        return true;
                    }

                    Player target = plugin.getServer().getPlayer(args[0]);

                    if (target == null) {
                        requester.sendMessage("§cPlayer not found!");
                        return true;
                    }

                    if (!plugin.getConfig().getBoolean(
                            "allow-tpa-to-yourself", false)
                            && requester.equals(target)) {

                        requester.sendMessage(
                                "§cYou cannot teleport yourself."
                        );
                        return true;
                    }

                    // Target is the player who will move.
                    String blocked = getBlockedMessage(
                            target,
                            requester,
                            WorldRestrictionManager.Action.TPAHERE
                    );

                    if (blocked != null) {
                        requester.sendMessage(blocked);
                        return true;
                    }

                    List<TpaRequest> list = pending.computeIfAbsent(
                            target.getUniqueId(),
                            k -> new ArrayList<>()
                    );

                    removeExpired(list);

                    for (TpaRequest req : list) {
                        if (req.getRequester().equals(
                                requester.getUniqueId())) {

                            requester.sendMessage(
                                    "§cYou already sent a TPA request. Type /tpacancel to cancel current TPA request"
                            );
                            return true;
                        }
                    }

                    list.add(new TpaRequest(
                            requester.getUniqueId(),
                            true,
                            getExpiryMillis()
                    ));

                    target.sendMessage(
                            "§b" + requester.getName()
                                    + " wants you to teleport to them. "
                                    + "Type /tpaccept"
                    );

                    playTpaSound(target);

                    requester.sendMessage(
                            "§bTpahere request sent to " + target.getName()
                    );

                    return true;
                }
        );

        // ==================== /tpaccept ====================

        plugin.getCommand("tpaccept").setExecutor(
                (sender, command, label, args) -> {

                    if (!(sender instanceof Player target)) {
                        return true;
                    }

                    List<TpaRequest> requests =
                            pending.get(target.getUniqueId());

                    if (requests == null || requests.isEmpty()) {
                        target.sendMessage("§cNo TPA request found.");
                        return true;
                    }

                    removeExpired(requests);

                    if (requests.isEmpty()) {
                        pending.remove(target.getUniqueId());
                        target.sendMessage("§cNo TPA request found.");
                        return true;
                    }

                    TpaRequest req = null;

                    if (args.length >= 1) {
                        Player wanted = plugin.getServer().getPlayer(args[0]);

                        if (wanted != null) {
                            for (TpaRequest r : requests) {
                                if (r.getRequester().equals(wanted.getUniqueId())) {
                                    req = r;
                                    break;
                                }
                            }
                        }

                        if (req == null) {
                            target.sendMessage("§cNo TPA request from that player.");
                            return true;
                        }

                        requests.remove(req);
                    } else {
                        req = requests.remove(0);
                    }

                    if (requests.isEmpty()) {
                        pending.remove(target.getUniqueId());
                    }

                    Player requester = plugin.getServer().getPlayer(
                            req.getRequester()
                    );

                    if (requester == null) {
                        target.sendMessage("§cRequester is offline.");
                        return true;
                    }

                    processTeleport(requester, target, req.isHere());

                    return true;
                }
        );

        // ==================== /tpacceptall ====================

        plugin.getCommand("tpacceptall").setExecutor(
                (sender, command, label, args) -> {

                    if (!(sender instanceof Player target)) {
                        return true;
                    }

                    List<TpaRequest> requests =
                            pending.remove(target.getUniqueId());

                    if (requests == null || requests.isEmpty()) {
                        target.sendMessage(
                                "§cNo TPA requests found."
                        );
                        return true;
                    }

                    int succeeded = 0;
                    int failed = 0;

                    for (TpaRequest req : requests) {

                        if (req.isExpired()) {
                            continue;
                        }

                        Player requester = plugin.getServer().getPlayer(
                                req.getRequester()
                        );

                        if (requester == null) {
                            failed++;
                            continue;
                        }

                        if (processTeleport(
                                requester, target, req.isHere())) {
                            succeeded++;
                        } else {
                            failed++;
                        }
                    }

                    target.sendMessage(
                            "§aSuccessfully teleported: " + succeeded
                                    + " §7| §cFailed: " + failed
                    );

                    return true;
                }
        );

        // ==================== /tpacancel ====================

        plugin.getCommand("tpacancel").setExecutor(
                (sender, command, label, args) -> {

                    if (!(sender instanceof Player requester)) {
                        return true;
                    }

                    UUID requesterId = requester.getUniqueId();
                    boolean[] cancelled = {false};

                    pending.entrySet().removeIf(entry -> {

                        entry.getValue().removeIf(req -> {
                            if (req.getRequester().equals(requesterId)) {
                                cancelled[0] = true;
                                return true;
                            }

                            return false;
                        });

                        return entry.getValue().isEmpty();
                    });

                    if (cancelled[0]) {
                        requester.sendMessage(
                                "§bYour TPA request has been cancelled."
                        );
                    } else {
                        requester.sendMessage(
                                "§cYou have no pending TPA requests to cancel."
                        );
                    }

                    return true;
                }
        );
    }

    // ==================== WORLD RESTRICTIONS ====================

    private String getBlockedMessage(
            Player movingPlayer,
            Player destinationPlayer,
            WorldRestrictionManager.Action action
    ) {
        return worldRestrictions.getBlockedMessage(
                movingPlayer.getWorld().getName(),
                destinationPlayer.getWorld().getName(),
                action
        );
    }

    private void removeExpired(List<TpaRequest> requests) {
        requests.removeIf(TpaRequest::isExpired);
    }

    // ==================== TELEPORT LOGIC ====================

    private boolean processTeleport(
            Player requester,
            Player target,
            boolean isHere
    ) {

        // Decide who moves and where they will go.
        Player movingPlayer = isHere ? target : requester;
        Player destinationPlayer = isHere ? requester : target;

        WorldRestrictionManager.Action action = isHere
                ? WorldRestrictionManager.Action.TPAHERE
                : WorldRestrictionManager.Action.TPA;

        // Recheck rules at acceptance time.
        String blocked = getBlockedMessage(
                movingPlayer,
                destinationPlayer,
                action
        );

        if (blocked != null) {
            requester.sendMessage(blocked);

            if (target.isOnline() && !target.equals(requester)) {
                target.sendMessage(blocked);
            }

            return false;
        }

        Economy econ = plugin.getEconomy();

        if (econ == null) {
            requester.sendMessage("§cEconomy is unavailable.");
            return false;
        }

        // Select the correct price for each request type.
        double cost = plugin.getConfig().getDouble(
                isHere ? "tpahere.def-cost" : "tpa.def-cost",
                10.0
        );

        if (!Double.isFinite(cost) || cost < 0) {
            requester.sendMessage("§cInvalid teleport cost configuration.");
            plugin.getLogger().warning(
                    "Invalid TPA cost configured for "
                            + (isHere ? "tpahere" : "tpa")
            );
            return false;
        }

        if (econ.getBalance(requester) < cost) {
            requester.sendMessage(
                    "§cYou do not have enough money. Cost: $" + cost
            );

            target.sendMessage(
                    "§c" + requester.getName()
                            + " does not have enough money."
            );

            return false;
        }

        // Charge requester only after restrictions pass.
        EconomyResponse payment = econ.withdrawPlayer(requester, cost);

        if (!payment.transactionSuccess()) {
            requester.sendMessage(
                    "§cPayment failed: " + payment.errorMessage
            );
            return false;
        }

        // Only report success if teleport succeeds.
        boolean teleported = movingPlayer.teleport(
                destinationPlayer.getLocation()
        );

        if (!teleported) {

            EconomyResponse refund = econ.depositPlayer(requester, cost);

            if (refund.transactionSuccess()) {
                requester.sendMessage(
                        "§cTeleport failed. Your money has been refunded."
                );
            } else {
                requester.sendMessage(
                        "§cTeleport failed and refund failed. "
                                + "Contact an administrator."
                );

                plugin.getLogger().severe(
                        "Refund failed for " + requester.getName()
                                + ", amount $" + cost
                                + ": " + refund.errorMessage
                );
            }

            return false;
        }

        // Success messages after a successful teleport.
        if (isHere) {

            requester.sendMessage(
                    "§b" + target.getName()
                            + " has teleported to you! Charged $" + cost
            );

            target.sendMessage(
                    "§bYou teleported to " + requester.getName() + "!"
            );

        } else {

            requester.sendMessage(
                    "§bTeleported to " + target.getName()
                            + "! Charged $" + cost
            );

            target.sendMessage(
                    "§b" + requester.getName()
                            + " has teleported to you!"
            );
        }

        return true;
    }

    // ==================== SOUND ====================

    private void playTpaSound(Player target) {

        if (!plugin.getConfig().getBoolean(
                "enable-sound-tpa", false)) {
            return;
        }

        target.playSound(
                target.getLocation(),
                getSound(),
                1f,
                1f
        );
    }

    private Sound getSound() {

        String soundName = plugin.getConfig().getString(
                "sound-tpa",
                "entity.experience_orb.pickup"
        );

        // Allow an old-style "Sound." prefix in config.
        if (soundName.startsWith("Sound.")) {
            soundName = soundName.substring("Sound.".length());
        }

        // fromString returns null for invalid input instead of throwing.
        NamespacedKey key = NamespacedKey.fromString(
                soundName.toLowerCase(Locale.ROOT)
        );

        Sound sound = key == null ? null : Registry.SOUNDS.get(key);

        return sound != null
                ? sound
                : Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    }
}
