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

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
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
import net.william278.velocitab.api.PlayerAddedToTabEvent;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * The main class for tracking the server TAB list
 */
public class PlayerTabList {
    private final Velocitab plugin;
    private final ConcurrentHashMap<UUID, TabPlayer> players;
    private final ConcurrentLinkedQueue<String> fallbackServers;
    private final List<UUID> justKicked;
    private ScheduledTask updateTask;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = new ConcurrentHashMap<>();
        this.fallbackServers = new ConcurrentLinkedQueue<>();
        this.justKicked = new CopyOnWriteArrayList<>();

        // If the update time is set to 0 do not schedule the updater
        if (plugin.getSettings().getUpdateRate() > 0) {
            this.updatePeriodically(plugin.getSettings().getUpdateRate());
        }
    }

    /**
     * Retrieves a TabPlayer object corresponding to the given Player object.
     *
     * @param player The Player object for which to retrieve the corresponding TabPlayer.
     * @return An Optional object containing the TabPlayer if found, or an empty Optional if not found.
     */
    public Optional<TabPlayer> getTabPlayer(@NotNull Player player) {
        return Optional.ofNullable(players.get(player.getUniqueId()));
    }


    /**
     * Loads the tab list for all players connected to the server.
     * Removes the player's entry from the tab list of all other players on the same group servers.
     */
    public void load() {
        plugin.getServer().getAllPlayers().forEach(p -> {
            final Optional<ServerConnection> server = p.getCurrentServer();
            if (server.isEmpty()) return;

            final List<RegisteredServer> serversInGroup = new ArrayList<>(getGroupServers(server.get().getServerInfo().getName()));
            if (serversInGroup.isEmpty()) return;

            serversInGroup.remove(server.get().getServer());

            joinPlayer(p, serversInGroup.stream().map(s -> s.getServerInfo().getName()).toList());
        });
    }

    /**
     * Closes the tab list for all players connected to the server.
     * Removes the player's entry from the tab list of all other players on the same group servers.
     */
    public void close() {
        plugin.getServer().getAllPlayers().forEach(p -> {
            final Optional<ServerConnection> server = p.getCurrentServer();
            if (server.isEmpty()) return;

            final List<RegisteredServer> serversInGroup = new ArrayList<>(getGroupServers(server.get().getServerInfo().getName()));
            if (serversInGroup.isEmpty()) return;

            serversInGroup.remove(server.get().getServer());

            serversInGroup.forEach(s -> s.getPlayersConnected().forEach(t -> t.getTabList().removeEntry(p.getUniqueId())));
        });
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        event.getPlayer().getTabList().clearAll();
        event.getPlayer().getTabList().clearHeaderAndFooter();
        justKicked.add(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final Player joined = event.getPlayer();
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(joined));

        final RegisteredServer previousServer = event.getPreviousServer();

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
            players.remove(event.getPlayer().getUniqueId());
            return;
        }

        joinPlayer(joined, serversInGroup.orElseGet(ArrayList::new));
    }

    private void joinPlayer(@NotNull Player joined, @NotNull List<String> serversInGroup) {
        // Add the player to the tracking list if they are not already listed
        final TabPlayer tabPlayer = getTabPlayer(joined).orElseGet(() -> createTabPlayer(joined));
        players.putIfAbsent(joined.getUniqueId(), tabPlayer);

        int delay = 500;

        if (justKicked.contains(joined.getUniqueId())) {
            delay = 1000;
            justKicked.remove(joined.getUniqueId());
        }

        //store last server so it's possible to have the last server on disconnect
        tabPlayer.setLastServer(joined.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(""));

        final boolean isVanished = plugin.getVanishManager().isVanished(joined.getUsername());
        // Update lists
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    final TabList tabList = joined.getTabList();
                    for (final TabPlayer player : players.values()) {
                        // Skip players on other servers if the setting is enabled
                        if (plugin.getSettings().isOnlyListPlayersInSameGroup()
                                && !serversInGroup.contains(player.getServerName())) {
                            continue;
                        }
                        // check if current player can see the joined player
                        if (!isVanished || plugin.getVanishManager().canSee(player.getPlayer().getUsername(), joined.getUsername())) {
                            addPlayerToTabList(player, tabPlayer);
                        } else {
                            player.getPlayer().getTabList().removeEntry(joined.getUniqueId());
                        }
                        // check if joined player can see current player
                        if ((plugin.getVanishManager().isVanished(player.getPlayer().getUsername()) &&
                                !plugin.getVanishManager().canSee(joined.getUsername(), player.getPlayer().getUsername())) && player.getPlayer() != joined) {
                            tabList.removeEntry(player.getPlayer().getUniqueId());
                        } else {
                            tabList.getEntry(player.getPlayer().getUniqueId()).ifPresentOrElse(
                                    entry -> player.getDisplayName(plugin).thenAccept(entry::setDisplayName)
                                            .exceptionally(throwable -> {
                                                plugin.log(Level.ERROR, String.format("Failed to set display name for %s (UUID: %s)",
                                                        player.getPlayer().getUsername(), player.getPlayer().getUniqueId()), throwable);
                                                return null;
                                            }),
                                    () -> createEntry(player, tabList).thenAccept(tabList::addEntry)
                            );
                        }

                        player.sendHeaderAndFooter(this);
                    }

                    plugin.getScoreboardManager().ifPresent(s -> {
                        s.resendAllTeams(joined);
                        tabPlayer.getTeamName(plugin).thenAccept(t -> s.updateRole(joined, t));
                    });

                    // Fire event without listening for result
                    plugin.getServer().getEventManager().fireAndForget(new PlayerAddedToTabEvent(tabPlayer, tabPlayer.getServerGroup(plugin), serversInGroup));
                })
                .delay(delay, TimeUnit.MILLISECONDS)
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

    private TabListEntry createEntry(@NotNull TabPlayer player, @NotNull TabList tabList, @NotNull Component displayName) {
        return TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(displayName)
                .latency(0)
                .tabList(tabList)
                .build();
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

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
            return;
        }

        // Remove the player from the tracking list, Print warning if player was not removed
        final UUID uuid = event.getPlayer().getUniqueId();
        final TabPlayer tabPlayer = players.get(uuid);
        if (tabPlayer == null) {
            plugin.log(String.format("Failed to remove disconnecting player %s (UUID: %s)",
                    event.getPlayer().getUsername(), uuid));
        }

        // Remove the player from the tab list of all other players
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(uuid));

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> players.values().forEach(player -> {
                    player.getPlayer().getTabList().removeEntry(uuid);
                    player.sendHeaderAndFooter(this);
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
        // Delete player team
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(event.getPlayer()));
        //remove player from tab list cache
        players.remove(uuid);
    }

    @NotNull
    public TabPlayer createTabPlayer(@NotNull Player player) {
        return new TabPlayer(player,
                plugin.getLuckPermsHook().map(hook -> hook.getPlayerRole(player)).orElse(Role.DEFAULT_ROLE)
        );
    }

    // Update a player's name in the tab list
    public void updatePlayer(@NotNull TabPlayer tabPlayer) {
        if (!tabPlayer.getPlayer().isActive()) {
            removeOfflinePlayer(tabPlayer.getPlayer());
            return;
        }

        tabPlayer.getTeamName(plugin).thenAccept(teamName -> {
            if (teamName.isBlank()) {
                return;
            }
            plugin.getScoreboardManager().ifPresent(manager -> manager.updateRole(
                    tabPlayer.getPlayer(), teamName
            ));
        });
    }

    public void updatePlayerDisplayName(@NotNull TabPlayer tabPlayer) {
        final Component lastDisplayName = tabPlayer.getLastDisplayname();
        tabPlayer.getDisplayName(plugin).thenAccept(displayName -> {
            if (displayName == null || displayName.equals(lastDisplayName)) {
                return;
            }

            final boolean isVanished = plugin.getVanishManager().isVanished(tabPlayer.getPlayer().getUsername());

            players.values().forEach(player -> {
                if (isVanished && !plugin.getVanishManager().canSee(player.getPlayer().getUsername(), tabPlayer.getPlayer().getUsername())) {
                    return;
                }

                player.getPlayer().getTabList().getEntries().stream()
                        .filter(e -> e.getProfile().getId().equals(tabPlayer.getPlayer().getUniqueId())).findFirst()
                        .ifPresent(entry -> entry.setDisplayName(displayName));
            });
        });
    }

    // Update the display names of all listed players
    public void updateDisplayNames() {
        players.values().forEach(this::updatePlayerDisplayName);
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
                    players.values().forEach(player -> {
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
            players.values().forEach(player -> {
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
    public Optional<List<String>> getGroupNames(@NotNull String serverName) {
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
    public List<RegisteredServer> getGroupServers(@NotNull String serverName) {
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
        players.remove(player.getUniqueId());
    }

    public void vanishPlayer(@NotNull TabPlayer tabPlayer) {
        players.values().forEach(p -> {
            if (p.getPlayer().equals(tabPlayer.getPlayer())) {
                return;
            }

            if (!plugin.getVanishManager().canSee(p.getPlayer().getUsername(), tabPlayer.getPlayer().getUsername())) {
                p.getPlayer().getTabList().removeEntry(tabPlayer.getPlayer().getUniqueId());
            }
        });
    }

    public void unVanishPlayer(@NotNull TabPlayer tabPlayer) {
        final UUID uuid = tabPlayer.getPlayer().getUniqueId();

        tabPlayer.getDisplayName(plugin).thenAccept(c -> players.values().forEach(p -> {
            if (p.getPlayer().equals(tabPlayer.getPlayer())) {
                return;
            }

            if (!p.getPlayer().getTabList().containsEntry(uuid)) {
                p.getPlayer().getTabList().addEntry(createEntry(tabPlayer, p.getPlayer().getTabList(), c));
            } else {
                p.getPlayer().getTabList().getEntry(uuid).ifPresent(entry -> entry.setDisplayName(c));
            }
        }));

    }

    /**
     * Recalculates the visibility of players in the tab list for the given player.
     * If tabPlayer can see the player, the player will be added to the tab list.
     *
     * @param tabPlayer The TabPlayer object representing the player for whom to recalculate the tab list visibility.
     */
    public void recalculateVanishForPlayer(@NotNull TabPlayer tabPlayer) {
        final Player player = tabPlayer.getPlayer();
        final Optional<List<String>> serversInGroupOptional = getGroupNames(player.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("?"));
        final List<String> serversInGroup = serversInGroupOptional.orElseGet(ArrayList::new);

        plugin.getServer().getAllPlayers().forEach(p -> {
            if (p.equals(player)) {
                return;
            }

            final Optional<TabPlayer> targetOptional = getTabPlayer(p);
            if (targetOptional.isEmpty()) {
                return;
            }

            final TabPlayer target = targetOptional.get();
            final String serverName = target.getServerName();

            if (plugin.getSettings().isOnlyListPlayersInSameGroup()
                    && !serversInGroup.contains(serverName)) {
                return;
            }

            final boolean canSee = !plugin.getVanishManager().isVanished(p.getUsername()) ||
                    plugin.getVanishManager().canSee(player.getUsername(), p.getUsername());

            if (!canSee) {
                player.getTabList().removeEntry(p.getUniqueId());
                plugin.getScoreboardManager().ifPresent(s -> s.recalculateVanishForPlayer(tabPlayer, target, false));
            } else {
                if (!player.getTabList().containsEntry(p.getUniqueId())) {
                    createEntry(target, player.getTabList()).thenAccept(e -> {
                        player.getTabList().addEntry(e);
                        plugin.getScoreboardManager().ifPresent(s -> s.recalculateVanishForPlayer(tabPlayer, target, true));
                    });
                }
            }
        });
    }
}
