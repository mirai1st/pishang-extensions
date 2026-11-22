package pishangext;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class UpdateChecker implements Listener {

    private static final String API_URL =
            "https://api.github.com/repos/mirai1st/pishang-extensions/releases/latest";
    private static final String RELEASE_URL =
            "https://github.com/mirai1st/pishang-extensions/releases/latest";

    private final Main plugin;
    private final HttpClient client = HttpClient.newHttpClient();

    // Only set when a newer version exists.
    private volatile String latestVersion;

    public UpdateChecker(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        check();
    }

    // Runs off the main thread, so it never lags the server.
    private void check() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "pishangext-update-checker")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        plugin.getLogger().info(
                                "Update check skipped (HTTP " + response.statusCode() + ")."
                        );
                        return;
                    }

                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String remote = json.get("tag_name").getAsString();
                    String current = plugin.getPluginMeta().getVersion();

                    if (isNewer(remote, current)) {
                        latestVersion = remote;
                        plugin.getLogger().warning(
                                "A new version is available: " + remote
                                        + " (current: " + current + ") - " + RELEASE_URL
                        );
                    } else {
                        plugin.getLogger().info("You are running the latest version.");
                    }
                })
                .exceptionally(e -> {
                    plugin.getLogger().info("Update check failed: " + e.getMessage());
                    return null;
                });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String latest = latestVersion;
        Player player = event.getPlayer();

        if (latest == null || !player.hasPermission("pishangext.admin")) {
            return;
        }

        // Small delay so the message does not get lost in the join spam.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage("§e[PishangExt] New version available: §a" + latest);
                player.sendMessage("§e" + RELEASE_URL);
            }
        }, 40L);
    }

    // Compares numbers only, e.g. "v2.1" > "2.0-b". Suffixes like "-b" are ignored.
    private static boolean isNewer(String remote, String current) {
        List<Integer> a = numbers(remote);
        List<Integer> b = numbers(current);
        int size = Math.max(a.size(), b.size());

        for (int i = 0; i < size; i++) {
            int x = i < a.size() ? a.get(i) : 0;
            int y = i < b.size() ? b.get(i) : 0;

            if (x != y) {
                return x > y;
            }
        }

        return false;
    }

    private static List<Integer> numbers(String version) {
        List<Integer> result = new ArrayList<>();

        for (String part : version.split("[^0-9]+")) {
            if (!part.isEmpty()) {
                result.add(Integer.parseInt(part));
            }
        }

        return result;
    }
}