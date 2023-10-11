/*
 * This file is part of Velocitab, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.velocitab.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * The main class for tracking the server TAB list
 */
public class PlayerTabList {
    private final Velocitab plugin;
    private final ConcurrentLinkedQueue<TabPlayer> players;
    private final ConcurrentLinkedQueue<String> fallbackServers;
    private ScheduledTask updateTask;

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
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(joined));

        // Remove the player from the tracking list if they are switching servers
        final RegisteredServer previousServer = event.getPreviousServer();
        if (previousServer != null) {
            players.removeIf(player -> player.getPlayer().getUniqueId().equals(joined.getUniqueId()));
        }

        // Get the servers in the group from the joined server name
        // If the server is not in a group, use fallback
        final Optional<List<String>> serversInGroup = getGroupNames(joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("?"));

        // If the server is not in a group, use fallback.
        // If fallback is disabled, permit the player to switch excluded servers without a header or footer override
        if (serversInGroup.isEmpty() &&
                (previousServer != null && !this.fallbackServers.contains(previousServer.getServerInfo().getName()))) {
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
                    for (TabPlayer player : players) {
                        // Skip players on other servers if the setting is enabled
                        if (plugin.getSettings().isOnlyListPlayersInSameGroup() && serversInGroup.isPresent()
                                && !serversInGroup.get().contains(player.getServerName())) {
                            continue;
                        }

                        tabList.getEntries().stream()
                                .filter(e -> e.getProfile().getId().equals(player.getPlayer().getUniqueId())).findFirst()
                                .ifPresentOrElse(
                                        entry -> player.getDisplayName(plugin).thenAccept(entry::setDisplayName),
                                        () -> createEntry(player, tabList).thenAccept(tabList::addEntry)
                                );
                        addPlayerToTabList(player, tabPlayer);

                        player.sendHeaderAndFooter(this);
                    }

                    plugin.getScoreboardManager().ifPresent(s -> {
                        s.resendAllNameTags(joined);
                        plugin.getTabPlayer(joined).getTeamName(plugin).thenAccept(t -> s.updateRole(joined, t));
                    });
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

    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
            return;
        }

        // Remove the player from the tracking list, Print warning if player was not removed
        final UUID uuid = event.getPlayer().getUniqueId();
        if (!players.removeIf(listed -> listed.getPlayer().getUniqueId().equals(uuid))) {
            plugin.log(String.format("Failed to remove disconnecting player %s (UUID: %s)",
                    event.getPlayer().getUsername(), uuid.toString()));
        }

        // Remove the player from the tab list of all other players
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(uuid));

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> players.forEach(player -> {
                    player.getPlayer().getTabList().removeEntry(uuid);
                    player.sendHeaderAndFooter(this);
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
        // Delete player team
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(event.getPlayer()));

    }

    // Replace a player in the tab list
    public void replacePlayer(@NotNull TabPlayer tabPlayer) {
        players.removeIf(player -> player.getPlayer().getUniqueId().equals(tabPlayer.getPlayer().getUniqueId()));
        players.add(tabPlayer);
    }

    // Update a player's name in the tab list
    public void updatePlayer(@NotNull TabPlayer tabPlayer) {
        if (!tabPlayer.getPlayer().isActive()) {
            removeOfflinePlayer(tabPlayer.getPlayer());
            return;
        }

        tabPlayer.getTeamName(plugin).thenAccept(teamName -> {
            plugin.getScoreboardManager().ifPresent(manager -> manager.updateRole(
                    tabPlayer.getPlayer(),
                    teamName
            ));
        });
    }

    public void updatePlayerDisplayName(TabPlayer tabPlayer) {
        final Component lastDisplayName = tabPlayer.getLastDisplayname();
        tabPlayer.getDisplayName(plugin).thenAccept(displayName -> {
            if (displayName == null || displayName.equals(lastDisplayName)) {
                return;
            }

            players.forEach(player ->
                    player.getPlayer().getTabList().getEntries().stream()
                            .filter(e -> e.getProfile().getId().equals(tabPlayer.getPlayer().getUniqueId())).findFirst()
                            .ifPresent(entry -> entry.setDisplayName(displayName)));
        });

    }

    // Update the display names of all listed players
    public void updateDisplayNames() {
        players.forEach(this::updatePlayerDisplayName);
    }

    // Get the component for the TAB list header
    public CompletableFuture<Component> getHeader(@NotNull TabPlayer player) {
        final String header = plugin.getSettings().getHeader(player.getServerGroup(plugin), player.getHeaderIndex());
        player.incrementHeaderIndex(plugin);

        return Placeholder.replace(header, plugin, player)
                .thenApply(replaced -> plugin.getFormatter().format(replaced, player, plugin));
    }

    // Get the component for the TAB list footer
    public CompletableFuture<Component> getFooter(@NotNull TabPlayer player) {
        final String footer = plugin.getSettings().getFooter(player.getServerGroup(plugin), player.getFooterIndex());
        player.incrementFooterIndex(plugin);

        return Placeholder.replace(footer, plugin, player)
                .thenApply(replaced -> plugin.getFormatter().format(replaced, player, plugin));
    }

    // Update the tab list periodically
    private void updatePeriodically(int updateRate) {
        updateTask = plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    if (players.isEmpty()) {
                        return;
                    }
                    players.forEach(player -> {
                        this.updatePlayer(player);
                        player.sendHeaderAndFooter(this);
                    });
                    updateDisplayNames();
                })
                .repeat(Math.max(200, updateRate), TimeUnit.MILLISECONDS)
                .schedule();
    }

    /**
     * Update the TAB list for all players when a plugin or proxy reload is performed
     */
    public void reloadUpdate() {
        if (players.isEmpty()) {
            return;
        }

        if (updateTask != null) {
            updateTask.cancel();
        }
        // If the update time is set to 0 do not schedule the updater
        if (plugin.getSettings().getUpdateRate() > 0) {
            this.updatePeriodically(plugin.getSettings().getUpdateRate());
        } else {
            players.forEach(player -> {
                this.updatePlayer(player);
                player.sendHeaderAndFooter(this);
            });
            updateDisplayNames();
        }

    }

    /**
     * Get the servers in the same group as the given server, as an optional
     * <p>
     * If the server is not in a group, use the fallback group
     * If the fallback is disabled, return an empty optional
     *
     * @param serverName The server name
     * @return The servers in the same group as the given server, empty if the server is not in a group and fallback is disabled
     */
    @NotNull
    public Optional<List<String>> getGroupNames(String serverName) {
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

    /**
     * Get the servers in the same group as the given server, as an optional list of {@link ServerInfo}
     * <p>
     * If the server is not in a group, use the fallback group
     * If the fallback is disabled, return an empty optional
     *
     * @param serverName The server name
     * @return The servers in the same group as the given server, empty if the server is not in a group and fallback is disabled
     */
    @NotNull
    public List<RegisteredServer> getGroupServers(String serverName) {
        return plugin.getServer().getAllServers().stream()
                .filter(server -> plugin.getSettings().getServerGroups().values().stream()
                        .filter(servers -> servers.contains(serverName))
                        .anyMatch(servers -> servers.contains(server.getServerInfo().getName())))
                .toList();
    }

    @Subscribe
    public void proxyReload(@NotNull ProxyReloadEvent event) {
        plugin.loadSettings();
        reloadUpdate();
        plugin.log("Velocitab has been reloaded!");
    }

    /**
     * Remove an offline player from the list of tracked TAB players
     *
     * @param player The player to remove
     */
    public void removeOfflinePlayer(@NotNull Player player) {
        players.removeIf(tabPlayer -> tabPlayer.getPlayer().getUniqueId().equals(player.getUniqueId()));
    }
}
