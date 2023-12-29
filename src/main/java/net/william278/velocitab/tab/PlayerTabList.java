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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import net.william278.velocitab.config.Group;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The main class for tracking the server TAB list
 */
public class PlayerTabList {
    private final Velocitab plugin;
    private final Map<UUID, TabPlayer> players;
    private final List<UUID> justKicked;
    private final Map<Group, ScheduledTask> placeholderTasks;
    private final Map<Group, ScheduledTask> headerFooterTasks;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = Maps.newConcurrentMap();
        this.justKicked = Lists.newCopyOnWriteArrayList();
        this.placeholderTasks = Maps.newConcurrentMap();
        this.headerFooterTasks = Maps.newConcurrentMap();
        this.reloadUpdate();
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

            final String serverName = server.get().getServerInfo().getName();
            final Group group = getGroup(serverName);
            final boolean isDefault = !group.servers().contains(serverName);

            if (isDefault && !plugin.getSettings().isFallbackEnabled()) {
                return;
            }

            joinPlayer(p, group);
        });
    }

    /**
     * Closes the tab list for all players connected to the server.
     * Removes the player's entry from the tab list of all other players on the same group servers.
     */
    public void close() {
        placeholderTasks.values().forEach(ScheduledTask::cancel);
        plugin.getServer().getAllPlayers().forEach(p -> {
            final Optional<ServerConnection> server = p.getCurrentServer();
            if (server.isEmpty()) return;

            final TabPlayer tabPlayer = players.get(p.getUniqueId());
            if (tabPlayer == null) {
                return;
            }

            final List<RegisteredServer> serversInGroup = new ArrayList<>(tabPlayer.getGroup().registeredServers(plugin));
            if (serversInGroup.isEmpty()) {
                return;
            }

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

        final String serverName = joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("");
        final Group group = getGroup(serverName);
        final boolean isDefault = !group.servers().contains(serverName);

        // If the server is not in a group, use fallback.
        // If fallback is disabled, permit the player to switch excluded servers without a header or footer override
        if (isDefault && !plugin.getSettings().isFallbackEnabled()) {
            event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            players.remove(event.getPlayer().getUniqueId());
            return;
        }

        joinPlayer(joined, group);
    }

    private void joinPlayer(@NotNull Player joined, @NotNull Group group) {
        // Add the player to the tracking list if they are not already listed
        final TabPlayer tabPlayer = getTabPlayer(joined).orElseGet(() -> createTabPlayer(joined, group));
        tabPlayer.setGroup(group);
        players.putIfAbsent(joined.getUniqueId(), tabPlayer);

        int delay = 500;

        if (justKicked.contains(joined.getUniqueId())) {
            delay = 1000;
            justKicked.remove(joined.getUniqueId());
        }

        //store last server, so it's possible to have the last server on disconnect
        tabPlayer.setLastServer(joined.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(""));

        final boolean isVanished = plugin.getVanishManager().isVanished(joined.getUsername());
        final boolean isDefault = group.isDefault();
        final boolean isFallback = isDefault && plugin.getSettings().isFallbackEnabled();
        // Update lists
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    final TabList tabList = joined.getTabList();
                    for (final TabPlayer player : players.values()) {
                        // Skip players on other servers if the setting is enabled
                        if (plugin.getSettings().isOnlyListPlayersInSameGroup()
                                && !isFallback &&
                                !group.servers().contains(player.getServerName())
                        ) {
                            System.out.println("Skipping " + player.getPlayer().getUsername() + " as they are on a different server");
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
                        s.resendAllTeams(tabPlayer);
                        tabPlayer.getTeamName(plugin).thenAccept(t -> s.updateRole(tabPlayer, t, false));
                    });

                    // Fire event without listening for result
                    plugin.getServer().getEventManager().fireAndForget(new PlayerAddedToTabEvent(tabPlayer, group));
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
    public TabPlayer createTabPlayer(@NotNull Player player, @NotNull Group group) {
        return new TabPlayer(player,
                plugin.getLuckPermsHook().map(hook -> hook.getPlayerRole(player)).orElse(Role.DEFAULT_ROLE),
                group
        );
    }

    // Update a player's name in the tab list and scoreboard team
    public void updatePlayer(@NotNull TabPlayer tabPlayer, boolean force) {
        if (!tabPlayer.getPlayer().isActive()) {
            removeOfflinePlayer(tabPlayer.getPlayer());
            return;
        }

        tabPlayer.getTeamName(plugin).thenAccept(teamName -> {
            if (teamName.isBlank()) {
                return;
            }
            plugin.getScoreboardManager().ifPresent(manager -> manager.updateRole(
                    tabPlayer, teamName, force
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
        final String header = player.getGroup().getHeader(player.getHeaderIndex());

        return Placeholder.replace(header, plugin, player)
                .thenApply(replaced -> plugin.getFormatter().format(replaced, player, plugin));
    }

    // Get the component for the TAB list footer
    public CompletableFuture<Component> getFooter(@NotNull TabPlayer player) {
        final String footer = player.getGroup().getFooter(player.getFooterIndex());

        return Placeholder.replace(footer, plugin, player)
                .thenApply(replaced -> plugin.getFormatter().format(replaced, player, plugin));
    }

    // Update the tab list periodically
    private void updatePeriodically(Group group) {
        cancelTasks(group);


        if (group.headerFooterUpdateRate() > 0) {
            final ScheduledTask headerFooterTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> {
                        group.getTabPlayers(plugin).forEach(TabPlayer::incrementIndexes);
                        updateGroupPlayers(group, false);
                    })
                    .repeat(Math.max(200, group.headerFooterUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            headerFooterTasks.put(group, headerFooterTask);
        }

        if (group.placeholderUpdateRate() > 0) {
            final ScheduledTask updateTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> updateGroupPlayers(group, true))
                    .repeat(Math.max(200, group.placeholderUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            placeholderTasks.put(group, updateTask);
        }
    }

    /**
     * Updates the players in the given group.
     *
     * @param group The group whose players should be updated.
     * @param all   Whether to update all player properties, or just the header and footer.
     */
    private void updateGroupPlayers(Group group, boolean all) {
        List<TabPlayer> groupPlayers = group.getTabPlayers(plugin);
        if (groupPlayers.isEmpty()) {
            return;
        }
        groupPlayers.forEach(player -> {
            if (all) {
                this.updatePlayer(player, false);
            }
            player.sendHeaderAndFooter(this);
        });
        if (all) {
            updateDisplayNames();
        }
    }

    private void cancelTasks(Group group) {
        ScheduledTask task = placeholderTasks.entrySet().stream()
                .filter(entry -> entry.getKey().equals(group))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (task != null) {
            task.cancel();
            placeholderTasks.remove(group);
        }

        task = headerFooterTasks.entrySet().stream()
                .filter(entry -> entry.getKey().equals(group))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (task != null) {
            task.cancel();
            headerFooterTasks.remove(group);
        }
    }

    /**
     * Update the TAB list for all players when a plugin or proxy reload is performed
     */
    public void reloadUpdate() {
        plugin.getTabGroups().getGroups().forEach(this::cancelTasks);
        plugin.getTabGroups().getGroups().forEach(this::updatePeriodically);

        if (players.isEmpty()) {
            return;
        }
        // If the update time is set to 0 do not schedule the updater
        players.values().forEach(player -> {
            this.updatePlayer(player, true);
            player.sendHeaderAndFooter(this);
        });
        updateDisplayNames();

    }

    @NotNull
    public Group getGroup(@NotNull String serverName) {
        return plugin.getTabGroups().getGroupFromServer(serverName);
    }

    @Subscribe
    public void proxyReload(@NotNull ProxyReloadEvent event) {
        plugin.loadConfigs();
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
        System.out.println("Removed " + player.getUsername() + " from tab list cache");
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
        final List<String> serversInGroup = tabPlayer.getGroup().servers();

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
