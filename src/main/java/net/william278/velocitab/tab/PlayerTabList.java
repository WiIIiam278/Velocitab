package net.william278.velocitab.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class PlayerTabList {
    private final Velocitab plugin;
    private final ConcurrentLinkedQueue<TabPlayer> players;
    private final ConcurrentLinkedQueue<String> fallbackServers;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = new ConcurrentLinkedQueue<>();
        this.fallbackServers = new ConcurrentLinkedQueue<>();

        // If the update time is set to 0 do not schedule the updater
        if (plugin.getSettings().getUpdateRate() > 0) {
            this.updatePeriodically(plugin.getSettings().getUpdateRate());
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final Player joined = event.getPlayer();
        plugin.getScoreboardManager().resetCache(joined);

        // Remove the player from the tracking list if they are switching servers
        if (event.getPreviousServer() == null) {
            players.removeIf(player -> player.getPlayer().getUniqueId().equals(joined.getUniqueId()));
        }

        // Get the servers in the group from the joined server name
        // If the server is not in a group, use fallback
        Optional<List<String>> serversInGroup = getSiblings(joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("?"));
        // If the server is not in a group, use fallback.
        // If fallback is disabled, permit the player to switch excluded servers without header or footer override
        if (serversInGroup.isEmpty() && !this.fallbackServers.contains(event.getPreviousServer().getServerInfo().getName())) {
            event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            return;
        }

        // Add the player to the tracking list
        final TabPlayer tabPlayer = plugin.getTabPlayer(joined);
        players.add(tabPlayer);

        // Update lists
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    final TabList tabList = joined.getTabList();
                    final Map<String, String> playerRoles = new HashMap<>();

                    for (TabPlayer player : players) {
                        if (serversInGroup.isPresent() && !serversInGroup.get().contains(player.getServerName())) {
                            continue; // Skip players on other servers
                        }
                        playerRoles.put(player.getPlayer().getUsername(), player.getTeamName(plugin));
                        tabList.getEntries().stream()
                                .filter(e -> e.getProfile().getId().equals(player.getPlayer().getUniqueId())).findFirst()
                                .ifPresentOrElse(
                                        entry -> player.getDisplayName(plugin).thenAccept(entry::setDisplayName),
                                        () -> createEntry(player, tabList).thenAccept(tabList::addEntry)
                                );
                        addPlayerToTabList(player, tabPlayer);
                        player.sendHeaderAndFooter(this);
                    }

                    plugin.getScoreboardManager().setRoles(joined, playerRoles);
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @NotNull
    private CompletableFuture<TabListEntry> createEntry(@NotNull TabPlayer player, @NotNull TabList tabList) {
        return player.getDisplayName(plugin).thenApply(name -> TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(name)
                .latency(0)
                .tabList(tabList)
                .build());
    }

    private void addPlayerToTabList(@NotNull TabPlayer player, @NotNull TabPlayer newPlayer) {
        if (newPlayer.getPlayer().getUniqueId().equals(player.getPlayer().getUniqueId())) {
            return;
        }

        player.getPlayer()
                .getTabList().getEntries().stream()
                .filter(e -> e.getProfile().getId().equals(newPlayer.getPlayer().getUniqueId())).findFirst()
                .ifPresentOrElse(
                        entry -> newPlayer.getDisplayName(plugin).thenAccept(entry::setDisplayName),
                        () -> createEntry(newPlayer, player.getPlayer().getTabList())
                                .thenAccept(entry -> player.getPlayer().getTabList().addEntry(entry))
                );
        plugin.getScoreboardManager().updateRoles(
                player.getPlayer(),
                newPlayer.getTeamName(plugin),
                newPlayer.getPlayer().getUsername()
        );
    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        // Remove the player from the tracking list
        players.removeIf(player -> player.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId()));

        // Remove the player from the tab list of all other players
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(event.getPlayer().getUniqueId()));

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> players.forEach(player -> {
                    player.getPlayer().getTabList().removeEntry(event.getPlayer().getUniqueId());
                    player.sendHeaderAndFooter(this);
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    // Replace a player in the tab list
    public void replacePlayer(@NotNull TabPlayer tabPlayer) {
        players.removeIf(player -> player.getPlayer().getUniqueId().equals(tabPlayer.getPlayer().getUniqueId()));
        players.add(tabPlayer);
    }

    // Update a player's name in the tab list
    public void updatePlayer(@NotNull TabPlayer tabPlayer) {
        players.forEach(player -> tabPlayer.getDisplayName(plugin).thenAccept(displayName -> {
            player.getPlayer().getTabList().getEntries().stream()
                    .filter(e -> e.getProfile().getId().equals(tabPlayer.getPlayer().getUniqueId())).findFirst()
                    .ifPresent(entry -> entry.setDisplayName(displayName));
            plugin.getScoreboardManager().updateRoles(player.getPlayer(),
                    tabPlayer.getTeamName(plugin), tabPlayer.getPlayer().getUsername());
        }));
    }

    public CompletableFuture<Component> getHeader(@NotNull TabPlayer player) {
        return Placeholder.replace(plugin.getSettings().getHeader(
                        plugin.getSettings().getServerGroup(player.getServerName())), plugin, player)
                .thenApply(header -> plugin.getFormatter().format(header, player, plugin));

    }

    public CompletableFuture<Component> getFooter(@NotNull TabPlayer player) {
        return Placeholder.replace(plugin.getSettings().getFooter(
                        plugin.getSettings().getServerGroup(player.getServerName())), plugin, player)
                .thenApply(footer -> plugin.getFormatter().format(footer, player, plugin));

    }

    // Update the tab list periodically
    private void updatePeriodically(int updateRate) {
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    if (players.isEmpty()) {
                        return;
                    }
                    players.forEach(player -> {
                        this.updatePlayer(player);
                        player.sendHeaderAndFooter(this);
                    });
                })
                .repeat(updateRate, TimeUnit.MILLISECONDS)
                .schedule();
    }

    /**
     * Get the servers in the same group as the given server
     * If the server is not in a group, use fallback
     * If fallback is disabled, return empty
     *
     * @param serverName The server name
     * @return The servers in the same group as the given server, empty if the server is not in a group and fallback is disabled
     */
    @NotNull
    public Optional<List<String>> getSiblings(String serverName) {
        return plugin.getSettings().getServerGroups().values().stream()
                .filter(servers -> servers.contains(serverName))
                .findFirst()
                .or(() -> {
                    if (!plugin.getSettings().isFallbackEnabled()) {
                        return Optional.empty();
                    }

                    if (!this.fallbackServers.contains(serverName)) {
                        this.fallbackServers.add(serverName);
                    }
                    return Optional.of(this.fallbackServers.stream().toList());
                });
    }
}
