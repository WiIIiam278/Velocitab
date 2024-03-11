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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.AccessLevel;
import lombok.Getter;
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
    @Getter
    private final VanishTabList vanishTabList;
    @Getter(value = AccessLevel.PUBLIC)
    private final Map<UUID, TabPlayer> players;
    private final Map<Group, ScheduledTask> placeholderTasks;
    private final Map<Group, ScheduledTask> headerFooterTasks;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.vanishTabList = new VanishTabList(plugin, this);
        this.players = Maps.newConcurrentMap();
        this.placeholderTasks = Maps.newConcurrentMap();
        this.headerFooterTasks = Maps.newConcurrentMap();
        this.reloadUpdate();
        this.registerListener();
    }

    private void registerListener() {
        plugin.getServer().getEventManager().register(plugin, new TabListListener(plugin, this));
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
     * Retrieves a TabPlayer object corresponding to the given UUID.
     *
     * @param uuid The UUID of the player for which to retrieve the corresponding TabPlayer.
     * @return An Optional object containing the TabPlayer if found, or an empty Optional if not found.
     */
    public Optional<TabPlayer> getTabPlayer(@NotNull UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
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
        placeholderTasks.clear();
        headerFooterTasks.values().forEach(ScheduledTask::cancel);
        headerFooterTasks.clear();
        plugin.getServer().getAllPlayers().forEach(p -> {
            final Optional<ServerConnection> server = p.getCurrentServer();
            if (server.isEmpty()) return;

            final TabPlayer tabPlayer = players.get(p.getUniqueId());
            if (tabPlayer == null) {
                return;
            }

            final Set<RegisteredServer> serversInGroup = tabPlayer.getGroup().registeredServers(plugin);
            if (serversInGroup.isEmpty()) {
                return;
            }

            serversInGroup.remove(server.get().getServer());
            serversInGroup.forEach(s -> s.getPlayersConnected().forEach(t -> t.getTabList().removeEntry(p.getUniqueId())));
        });
    }


    protected void joinPlayer(@NotNull Player joined, @NotNull Group group) {
        // Add the player to the tracking list if they are not already listed
        final TabPlayer tabPlayer = getTabPlayer(joined).orElseGet(() -> createTabPlayer(joined, group));
        tabPlayer.setGroup(group);
        players.putIfAbsent(joined.getUniqueId(), tabPlayer);

        //store last server, so it's possible to have the last server on disconnect
        tabPlayer.setLastServer(joined.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(""));

        final boolean isVanished = plugin.getVanishManager().isVanished(joined.getUsername());
        final boolean isDefault = group.isDefault();
        final boolean isFallback = isDefault && plugin.getSettings().isFallbackEnabled();

        tabPlayer.getDisplayName(plugin).thenAccept(d -> {

            joined.getTabList().getEntry(joined.getUniqueId())
                    .ifPresentOrElse(e -> e.setDisplayName(d),
                            () -> joined.getTabList().addEntry(createEntry(tabPlayer, joined.getTabList(), d)));

            tabPlayer.sendHeaderAndFooter(this)
                    .thenAccept(v -> tabPlayer.setLoaded(true))
                    .exceptionally(throwable -> {
                        plugin.log(Level.ERROR, String.format("Failed to send header and footer for %s (UUID: %s)",
                                joined.getUsername(), joined.getUniqueId()), throwable);
                        return null;
                    });

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
                                continue;
                            }
                            // check if current player can see the joined player
                            if (!isVanished || plugin.getVanishManager().canSee(player.getPlayer().getUsername(), joined.getUsername())) {
                                addPlayerToTabList(player, tabPlayer, d);
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
                    .delay(300, TimeUnit.MILLISECONDS)
                    .schedule();
        }).exceptionally(throwable -> {
            plugin.log(Level.ERROR, String.format("Failed to set display name for %s (UUID: %s)",
                    joined.getUsername(), joined.getUniqueId()), throwable);
            return null;
        });
    }

    protected void removePlayer(@NotNull Player target) {
        final UUID uuid = target.getUniqueId();
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(uuid));

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> getPlayers().values().forEach(player -> {
                    player.getPlayer().getTabList().removeEntry(uuid);
                    player.sendHeaderAndFooter(this);
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
        // Delete player team
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(target));
        //remove player from tab list cache
        getPlayers().remove(uuid);
    }

    @NotNull
    CompletableFuture<TabListEntry> createEntry(@NotNull TabPlayer player, @NotNull TabList tabList) {
        return player.getDisplayName(plugin).thenApply(name -> TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(name)
                .latency(0)
                .tabList(tabList)
                .build());
    }

    protected TabListEntry createEntry(@NotNull TabPlayer player, @NotNull TabList tabList, @NotNull Component displayName) {
        return TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(displayName)
                .latency(0)
                .tabList(tabList)
                .build();
    }

    private void addPlayerToTabList(@NotNull TabPlayer player, @NotNull TabPlayer newPlayer, @NotNull Component displayName) {
        if (newPlayer.getPlayer().getUniqueId().equals(player.getPlayer().getUniqueId())) {
            return;
        }

        plugin.getPacketEventManager().getVelocitabEntries().add(newPlayer.getPlayer().getUniqueId());

        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> plugin.getPacketEventManager().getVelocitabEntries().remove(newPlayer.getPlayer().getUniqueId()))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();

        player.getPlayer()
                .getTabList().getEntries().stream()
                .filter(e -> e.getProfile().getId().equals(newPlayer.getPlayer().getUniqueId())).findFirst()
                .ifPresentOrElse(
                        entry -> entry.setDisplayName(displayName),
                        () -> player.getPlayer().getTabList()
                                .addEntry(createEntry(newPlayer, player.getPlayer().getTabList(), displayName))
                );
    }


    @NotNull
    public TabPlayer createTabPlayer(@NotNull Player player, @NotNull Group group) {
        return new TabPlayer(plugin, player,
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
        final Component lastDisplayName = tabPlayer.getLastDisplayName();
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
                    .buildTask(plugin, () -> updateGroupPlayers(group, false, true))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.headerFooterUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            headerFooterTasks.put(group, headerFooterTask);
        }

        if (group.placeholderUpdateRate() > 0) {
            final ScheduledTask updateTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> updateGroupPlayers(group, true, false))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.placeholderUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            placeholderTasks.put(group, updateTask);
        }
    }

    /**
     * Updates the players in the given group.
     *
     * @param group            The group whose players should be updated.
     * @param all              Whether to update all player properties, or just the header and footer.
     * @param incrementIndexes Whether to increment the header and footer indexes.
     */
    private void updateGroupPlayers(@NotNull Group group, boolean all, boolean incrementIndexes) {
        Set<TabPlayer> groupPlayers = group.getTabPlayers(plugin);
        if (groupPlayers.isEmpty()) {
            return;
        }
        groupPlayers.stream()
                .filter(player -> player.getPlayer().isActive())
                .forEach(player -> {
                    if (incrementIndexes) {
                        player.incrementIndexes();
                    }
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
        placeholderTasks.values().forEach(ScheduledTask::cancel);
        placeholderTasks.clear();
        plugin.getTabGroups().getGroups().forEach(this::updatePeriodically);

        if (players.isEmpty()) {
            return;
        }
        // If the update time is set to 0 do not schedule the updater
        players.values().forEach(player -> {
            final Optional<ServerConnection> server = player.getPlayer().getCurrentServer();
            if (server.isEmpty()) {
                return;
            }
            final String serverName = server.get().getServerInfo().getName();
            final Group group = getGroup(serverName);
            player.setGroup(group);
            this.updatePlayer(player, true);
            player.sendHeaderAndFooter(this);
        });
        updateDisplayNames();
    }

    @NotNull
    public Group getGroup(@NotNull String serverName) {
        return plugin.getTabGroups().getGroupFromServer(serverName);
    }


    /**
     * Remove an offline player from the list of tracked TAB players
     *
     * @param player The player to remove
     */
    public void removeOfflinePlayer(@NotNull Player player) {
        players.remove(player.getUniqueId());
    }

}
