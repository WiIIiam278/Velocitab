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

import com.google.common.collect.Maps;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.api.PlayerAddedToTabEvent;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.config.ServerUrl;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket.Action.UPDATE_LIST_ORDER;

/**
 * The main class for tracking the server TAB list for a map of {@link TabPlayer}s
 */
public class PlayerTabList {

    private final Velocitab plugin;
    @Getter
    private final VanishTabList vanishTabList;
    @Getter(value = AccessLevel.PUBLIC)
    private final Map<UUID, TabPlayer> players;
    private final TaskManager taskManager;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.vanishTabList = new VanishTabList(plugin, this);
        this.players = Maps.newConcurrentMap();
        this.taskManager = new TaskManager(plugin);
        this.reloadUpdate();
        this.registerListener();
        this.ensureDisplayNameTask();
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
            if (server.isEmpty()) {
                return;
            }

            final String serverName = server.get().getServerInfo().getName();
            final @NotNull Optional<Group> group = getGroup(serverName);
            if (group.isEmpty()) {
                return;
            }

            joinPlayer(p, group.get());
        });
    }

    /**
     * Closes the tab list for all players connected to the server.
     * Removes the player's entry from the tab list of all other players on the same group servers.
     */
    public void close() {
        taskManager.cancelAllTasks();
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

    protected void clearCachedData(@NotNull Player player) {
        players.values().forEach(p -> {
            p.unsetRelationalDisplayName(player.getUniqueId());
            p.unsetRelationalNametag(player.getUniqueId());
        });
    }


    protected void joinPlayer(@NotNull Player joined, @NotNull Group group) {
        // Add the player to the tracking list if they are not already listed
        final Optional<TabPlayer> tabPlayerOptional = getTabPlayer(joined);
        if (tabPlayerOptional.isPresent()) {
            tabPlayerOptional.get().clearCachedData();
            tabPlayerOptional.get().setGroup(group);
            tabPlayerOptional.get().setRole(plugin.getLuckPermsHook().map(hook -> hook.getPlayerRole(joined)).orElse(Role.DEFAULT_ROLE));
        }
        final TabPlayer tabPlayer = tabPlayerOptional.orElseGet(() -> createTabPlayer(joined, group));
        final String serverName = getServerName(joined);
        // Store last server, so it's possible to have the last server on disconnect
        tabPlayer.setLastServer(serverName);

        // Send server URLs (1.21 clients)
        sendPlayerServerLinks(tabPlayer);

        // Set the player as not loaded until the display name is set
        tabPlayer.getDisplayName(plugin).thenAccept(d -> {
            if (d == null) {
                plugin.log(Level.ERROR, "Failed to get display name for " + joined.getUsername());
                return;
            }

            handleDisplayLoad(tabPlayer);
        }).exceptionally(throwable -> {
            plugin.log(Level.ERROR, String.format("Failed to set display name for %s (UUID: %s)",
                    joined.getUsername(), joined.getUniqueId()), throwable);
            return null;
        });
    }

    private void handleDisplayLoad(@NotNull TabPlayer tabPlayer) {
        final Player joined = tabPlayer.getPlayer();
        final Group group = tabPlayer.getGroup();
        final boolean isVanished = plugin.getVanishManager().isVanished(joined.getUsername());
        players.putIfAbsent(joined.getUniqueId(), tabPlayer);
        tabPlayer.sendHeaderAndFooter(this)
                .thenAccept(v -> tabPlayer.setLoaded(true))
                .exceptionally(throwable -> {
                    plugin.log(Level.ERROR, String.format("Failed to send header and footer for %s (UUID: %s)",
                            joined.getUsername(), joined.getUniqueId()), throwable);
                    return null;
                });

        final Set<TabPlayer> tabPlayers = group.getTabPlayers(plugin, tabPlayer);
        updateTabListOnJoin(tabPlayer, group, tabPlayers, isVanished);
    }

    private void updateTabListOnJoin(@NotNull TabPlayer tabPlayer, @NotNull Group group,
                                     @NotNull Set<TabPlayer> tabPlayers, boolean isJoinedVanished) {
        final Player joined = tabPlayer.getPlayer();
        final String serverName = getServerName(joined);
        final Set<UUID> uuids = tabPlayers.stream().map(p -> p.getPlayer().getUniqueId()).collect(Collectors.toSet());
        List.copyOf(tabPlayer.getPlayer().getTabList().getEntries()).forEach(entry -> {
            if (!uuids.contains(entry.getProfile().getId())) {
                tabPlayer.getPlayer().getTabList().removeEntry(entry.getProfile().getId());
            }
        });
        for (final TabPlayer iteratedPlayer : tabPlayers) {
            final Player player = iteratedPlayer.getPlayer();
            final String username = player.getUsername();
            final boolean isPlayerVanished = plugin.getVanishManager().isVanished(username);

            if (group.onlyListPlayersInSameServer() && !serverName.equals(getServerName(player))) {
                continue;
            }

            // Update lists regarding the joined player
            checkVisibilityAndUpdateName(iteratedPlayer, tabPlayer, isJoinedVanished);
            // Update lists regarding the iterated player
            if (iteratedPlayer != tabPlayer) {
                checkVisibilityAndUpdateName(tabPlayer, iteratedPlayer, isPlayerVanished);
            }
            iteratedPlayer.sendHeaderAndFooter(this);
        }
        final ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        scoreboardManager.resendAllTeams(tabPlayer);
        updateSorting(tabPlayer, false);
        fixDuplicateEntries(joined);
        // Fire event without listening for result
        plugin.getServer().getEventManager().fireAndForget(new PlayerAddedToTabEvent(tabPlayer, group));
    }

    private void checkVisibilityAndUpdateName(@NotNull TabPlayer observedPlayer, @NotNull TabPlayer observableTabPlayer,
                                              boolean isObservablePlayerVanished) {
        final UUID observableUUID = observableTabPlayer.getPlayer().getUniqueId();
        final String observedUsername = observedPlayer.getPlayer().getUsername();
        final String observableUsername = observableTabPlayer.getPlayer().getUsername();
        final TabList observableTabPlayerTabList = observableTabPlayer.getPlayer().getTabList();

        if (isObservablePlayerVanished && !plugin.getVanishManager().canSee(observableUsername, observedUsername) &&
            !observableUUID.equals(observedPlayer.getPlayer().getUniqueId())) {
            observableTabPlayerTabList.removeEntry(observedPlayer.getPlayer().getUniqueId());
        } else {
            updateDisplayName(observedPlayer, observableTabPlayer);
        }
    }

    @NotNull
    private String getServerName(@NotNull Player player) {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("");
    }

    @NotNull
    public Component getRelationalPlaceholder(@NotNull TabPlayer player, @NotNull TabPlayer viewer,
                                              @NotNull Component single, @NotNull String toParse) {
        if (plugin.getMiniPlaceholdersHook().isEmpty()) {
            return single;
        }
        return plugin.getFormatter().format(toParse, player, viewer, plugin);
    }

    @NotNull
    public Component getRelationalPlaceholder(@NotNull TabPlayer player, @NotNull TabPlayer viewer, @NotNull String toParse) {
        final Component single = plugin.getFormatter().format(toParse, player, viewer, plugin);
        return getRelationalPlaceholder(player, viewer, single, toParse);
    }

    @SuppressWarnings("unchecked")
    private void fixDuplicateEntries(@NotNull Player target) {
        try {
            final Field entriesField = target.getTabList().getClass().getDeclaredField("entries");
            entriesField.setAccessible(true);
            final Map<UUID, TabListEntry> entries = (Map<UUID, TabListEntry>) entriesField.get(target.getTabList());
            entries.entrySet().stream()
                    .filter(entry -> entry.getValue().getProfile() != null)
                    .filter(entry -> entry.getValue().getProfile().getId().equals(target.getUniqueId()))
                    .filter(entry -> !entry.getKey().equals(target.getUniqueId()))
                    .forEach(entry -> target.getTabList().removeEntry(entry.getKey()));
        } catch (Throwable error) {
            plugin.log(Level.ERROR, "Failed to fix duplicate entries for class " + target.getTabList().getClass().getName(), error);
        }
    }

    protected void removePlayer(@NotNull Player target) {
        removePlayer(target, null);
    }

    /**
     * Remove a player from the tab list
     *
     * @param uuid {@link UUID} of the {@link TabPlayer player} to remove
     */
    protected void removeTabListUUID(@NotNull UUID uuid) {
        getPlayers().forEach((key, value) -> value.getPlayer().getTabList().getEntry(uuid).ifPresent(
                entry -> value.getPlayer().getTabList().removeEntry(uuid)
        ));
    }

    protected void removePlayer(@NotNull Player target, @Nullable RegisteredServer server) {
        final UUID uuid = target.getUniqueId();
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(uuid));

        final Set<Player> currentServerPlayers = Optional.ofNullable(server)
                .map(RegisteredServer::getPlayersConnected)
                .map(HashSet::new)
                .orElseGet(HashSet::new);
        currentServerPlayers.add(target);

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> getPlayers().values().stream()
                        .filter(p -> currentServerPlayers.isEmpty() || !currentServerPlayers.contains(p.getPlayer()))
                        .forEach(player -> {
                            player.getPlayer().getTabList().removeEntry(uuid);
                            player.sendHeaderAndFooter(this);
                            updatePlayerDisplayName(player);
                        }))
                .delay(250, TimeUnit.MILLISECONDS)
                .schedule();
        // Delete player team
        plugin.getScoreboardManager().resetCache(target);
        //remove player from tab list cache
        getPlayers().remove(uuid);
    }

    @NotNull
    protected TabListEntry createEntry(@NotNull TabPlayer player, @NotNull TabList tabList, @NotNull Component displayName) {
        return TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(displayName)
                .latency(Math.max((int) player.getPlayer().getPing(), 0))
                .tabList(tabList)
                .build();
    }

    @NotNull
    protected TabListEntry createEntry(@NotNull TabPlayer player, @NotNull TabList tabList, @NotNull TabPlayer viewer) {
        if (!viewer.getPlayer().getTabList().equals(tabList)) {
            throw new IllegalArgumentException("TabList of viewer is not the same as the TabList of the entry");
        }
        final Component single = plugin.getFormatter().format(player.getLastDisplayName(), player, viewer, plugin);
        final Component displayName = getRelationalPlaceholder(player, viewer, single, player.getGroup().format());
        player.setRelationalDisplayName(viewer.getPlayer().getUniqueId(), displayName);
        return TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(displayName)
                .latency(Math.max((int) player.getPlayer().getPing(), 0))
                .tabList(tabList)
                .build();
    }

    protected void updateDisplayName(@NotNull TabPlayer player, @NotNull TabPlayer viewer) {
        final Component displayName = getRelationalPlaceholder(player, viewer, player.getLastDisplayName());
        updateDisplayName(player, viewer, displayName);
    }

    protected void updateDisplayName(@NotNull TabPlayer player, @NotNull TabPlayer viewer, @NotNull Component displayName) {
        final Optional<Component> cached = player.getRelationalDisplayName(viewer.getPlayer().getUniqueId());
        if (cached.isPresent() && cached.get().equals(displayName) &&
            viewer.getPlayer().getTabList().getEntry(player.getPlayer().getUniqueId())
                    .flatMap(TabListEntry::getDisplayNameComponent).map(displayName::equals)
                    .orElse(false)
        ) {
            return;
        }

        player.setRelationalDisplayName(viewer.getPlayer().getUniqueId(), displayName);
        viewer.getPlayer().getTabList().getEntry(player.getPlayer().getUniqueId())
                .ifPresentOrElse(
                        entry -> entry.setDisplayName(displayName),
                        () -> viewer.getPlayer().getTabList()
                                .addEntry(createEntry(player, viewer.getPlayer().getTabList(), displayName))
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

        updateSorting(tabPlayer, force);
    }

    private void updateSorting(@NotNull TabPlayer tabPlayer, boolean force) {
        tabPlayer.getTeamName(plugin).thenAccept(teamName -> {
            if (teamName.isBlank()) {
                return;
            }
            plugin.getScoreboardManager().updateRole(
                    tabPlayer, teamName, force
            );
            final int order = plugin.getScoreboardManager().getPosition(teamName);
            if (order == -1) {
                return;
            }
            tabPlayer.getGroup().getTabPlayers(plugin, tabPlayer).forEach(p -> {
                if (!hasListOrder(p)) {
                    return;
                }
                updateSorting(p, p.getPlayer().getUniqueId(), order);
            });
        });
    }

    public void sendPlayerServerLinks(@NotNull TabPlayer player) {
        if (player.getPlayer().getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21)) {
            return;
        }
        final List<ServerUrl> urls = plugin.getSettings().getUrlsForGroup(player.getGroup());
        ServerUrl.resolve(plugin, player, urls).thenAccept(player.getPlayer()::setServerLinks);
    }

    public void updatePlayerDisplayName(@NotNull TabPlayer tabPlayer) {
        tabPlayer.getDisplayName(plugin).thenAccept(displayName -> {
            if (displayName == null) {
                plugin.log(Level.ERROR, "Failed to get display name for " + tabPlayer.getPlayer().getUsername());
                return;
            }
            final Component single = plugin.getFormatter().format(displayName, tabPlayer, plugin);

            final boolean isVanished = plugin.getVanishManager().isVanished(tabPlayer.getPlayer().getUsername());
            final Set<TabPlayer> players = tabPlayer.getGroup().getTabPlayers(plugin, tabPlayer);

            players.forEach(player -> {
                if (isVanished && !plugin.getVanishManager().canSee(player.getPlayer().getUsername(), tabPlayer.getPlayer().getUsername())) {
                    return;
                }

                final Component relationalPlaceholder = getRelationalPlaceholder(tabPlayer, player, single, displayName);
                updateDisplayName(tabPlayer, player, relationalPlaceholder);
            });
        });
    }

    public void checkCorrectDisplayName(@NotNull TabPlayer tabPlayer) {
        if (!tabPlayer.isLoaded()) {
            return;
        }
        final boolean bypass = plugin.getSettings().isForceSendingTabListPackets();
        players.values()
                .stream()
                .filter(TabPlayer::isLoaded)
                .forEach(player -> player.getPlayer().getTabList().getEntry(tabPlayer.getPlayer().getUniqueId())
                        .ifPresent(entry -> {
                            final Optional<Component> displayNameOptional = tabPlayer.getRelationalDisplayName(player.getPlayer().getUniqueId());
                            if (displayNameOptional.isEmpty()) {
                                return;
                            }

                            final Component lastDisplayName = displayNameOptional.get();
                            if (bypass || entry.getDisplayNameComponent().isEmpty() || !lastDisplayName.equals(entry.getDisplayNameComponent().get())) {
                                entry.setDisplayName(lastDisplayName);
                            }
                        }));
    }


    // Update the display names of all listed players
    public void updateDisplayNames() {
        players.values().forEach(this::updatePlayerDisplayName);
    }

    public void checkCorrectDisplayNames() {
        players.values().forEach(this::checkCorrectDisplayName);
    }

    public void ensureDisplayNameTask() {
        plugin.getServer().getScheduler()
                .buildTask(plugin, this::checkCorrectDisplayNames)
                .delay(1, TimeUnit.SECONDS)
                .repeat(2, TimeUnit.SECONDS)
                .schedule();
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

    /**
     * Update the TAB list for all players when a plugin or proxy reload is performed
     */
    public void reloadUpdate() {
        taskManager.cancelAllTasks();
        plugin.getTabGroups().getGroups().forEach(taskManager::updatePeriodically);
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
            final Optional<Group> group = getGroup(serverName);
            if (group.isEmpty()) {
                return;
            }
            player.setGroup(group.get());
            this.sendPlayerServerLinks(player);
            this.updatePlayer(player, true);
            player.sendHeaderAndFooter(this);
        });
        updateDisplayNames();
    }

    @NotNull
    public Optional<Group> getGroup(@NotNull String serverName) {
        return plugin.getTabGroups().getGroupFromServer(serverName, plugin);
    }

    public void removeOldEntry(@NotNull Group group, @NotNull UUID uuid) {
        final Set<TabPlayer> players = group.getTabPlayers(plugin);
        players.forEach(player -> player.getPlayer().getTabList().removeEntry(uuid));
    }

    /**
     * Remove an offline player from the list of tracked TAB players
     *
     * @param player The player to remove
     */
    public void removeOfflinePlayer(@NotNull Player player) {
        players.remove(player.getUniqueId());
    }

    /**
     * Whether the player can use server-side specified TAB list ordering (Minecraft 1.21.2+)
     *
     * @param tabPlayer player to check
     * @return {@code true} if the user is on Minecraft 1.21.2+; {@code false}
     */
    private boolean hasListOrder(@NotNull TabPlayer tabPlayer) {
        return tabPlayer.getPlayer().getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_21_2);
    }

    private void updateSorting(@NotNull TabPlayer tabPlayer, @NotNull UUID uuid, int position) {
        final UpsertPlayerInfoPacket packet = new UpsertPlayerInfoPacket(UPDATE_LIST_ORDER);
        final UpsertPlayerInfoPacket.Entry entry = new UpsertPlayerInfoPacket.Entry(uuid);
        entry.setListOrder(position);
        packet.addEntry(entry);
        ((ConnectedPlayer) tabPlayer.getPlayer()).getConnection().write(packet);
    }

}
