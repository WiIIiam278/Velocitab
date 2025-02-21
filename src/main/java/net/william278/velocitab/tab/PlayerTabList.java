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
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.util.ServerLink;
import com.velocitypowered.proxy.tablist.KeyedVelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.api.PlayerAddedToTabEvent;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.config.ServerUrl;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The main class for tracking the server TAB list for a map of {@link TabPlayer}s
 */
public class PlayerTabList {

    private static final String RELATIONAL_PERMISSION = "velocitab.relational";

    private final Velocitab plugin;
    @Getter
    private final VanishTabList vanishTabList;
    @Getter(value = AccessLevel.PUBLIC)
    private final Map<UUID, TabPlayer> players;
    @Getter(value = AccessLevel.PROTECTED)
    private final TaskManager taskManager;
    private final Map<Class<?>, Field> entriesFields;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.vanishTabList = new VanishTabList(plugin, this);
        this.players = Maps.newConcurrentMap();
        this.taskManager = new TaskManager(plugin);
        this.entriesFields = Maps.newHashMap();
        this.registerListener();
        this.ensureDisplayNameTask();
        this.registerFields();
    }

    // VelocityTabListLegacy is not supported
    private void registerFields() {
        final Class<KeyedVelocityTabList> keyedVelocityTabListClass = KeyedVelocityTabList.class;
        final Class<VelocityTabList> velocityTabListClass = VelocityTabList.class;
        try {
            final Field entriesField = keyedVelocityTabListClass.getDeclaredField("entries");
            entriesField.setAccessible(true);
            this.entriesFields.put(keyedVelocityTabListClass, entriesField);
        } catch (NoSuchFieldException e) {
            plugin.log(Level.ERROR, "Failed to register KeyedVelocityTabList field", e);
        }
        try {
            final Field entriesField = velocityTabListClass.getDeclaredField("entries");
            entriesField.setAccessible(true);
            this.entriesFields.put(velocityTabListClass, entriesField);
        } catch (NoSuchFieldException e) {
            plugin.log(Level.ERROR, "Failed to register VelocityTabList field", e);
        }
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

            loadPlayer(p, group.get(), 400);
        });

        reloadUpdate();
    }

    protected void loadPlayer(@NotNull Player player, @NotNull Group group, int delay) {
        final ScheduledTask task = plugin.getServer().getScheduler()
                .buildTask(plugin, () -> plugin.getPlaceholderManager().fetchPlaceholders(player.getUniqueId(), group.getTextsWithPlaceholders(), group))
                .delay(150, TimeUnit.MILLISECONDS)
                .repeat(50, TimeUnit.MILLISECONDS)
                .schedule();

        //After updating papiproxybridge we can check if redis is used
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
                    task.cancel();
                    joinPlayer(player, group);
                })
                .delay(delay, TimeUnit.MILLISECONDS)
                .schedule();
    }

    /**
     * Closes the tab list for all players connected to the server.
     * Removes the player's entry from the tab list of all other players on the same group servers.
     */
    public void close() {
        taskManager.close();
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
        plugin.getPacketEventManager().removeAllPlayers();
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

        handleDisplayLoad(tabPlayer);
    }

    private void handleDisplayLoad(@NotNull TabPlayer tabPlayer) {
        final Player joined = tabPlayer.getPlayer();
        final Group group = tabPlayer.getGroup();
        final boolean isVanished = plugin.getVanishManager().isVanished(joined.getUsername());
        players.putIfAbsent(joined.getUniqueId(), tabPlayer);
        tabPlayer.sendHeaderAndFooter(this);
        tabPlayer.setLoaded(true);
        final List<TabPlayer> tabPlayers = group.getTabPlayersAsList(plugin, tabPlayer);
        updateTabListOnJoin(tabPlayer, group, tabPlayers, isVanished);
    }

    private void updateTabListOnJoin(@NotNull TabPlayer tabPlayer, @NotNull Group group,
                                     @NotNull List<TabPlayer> tabPlayers, boolean isJoinedVanished) {
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

        if ((isObservablePlayerVanished && !plugin.getVanishManager().canSee(observableUsername, observedUsername) &&
                !observableUUID.equals(observedPlayer.getPlayer().getUniqueId())) || !observedPlayer.getPlayer().isActive()) {
            observableTabPlayerTabList.removeEntry(observedPlayer.getPlayer().getUniqueId());
        } else {
            calculateAndSetDisplayName(observedPlayer, observableTabPlayer);
        }
    }

    @NotNull
    private String getServerName(@NotNull Player player) {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("");
    }

    @NotNull
    public Component formatRelationalComponent(@NotNull TabPlayer player, @NotNull TabPlayer viewer,
                                               @NotNull String toParse) {
        return plugin.getFormatter().format(toParse, player, viewer, plugin);
    }

    @NotNull
    public Component formatComponent(@NotNull TabPlayer player, @NotNull String toParse) {
        return plugin.getFormatter().format(toParse, player, plugin);
    }

    @SuppressWarnings("unchecked")
    private void fixDuplicateEntries(@NotNull Player target) {
        try {
            final Optional<Field> optionalField = Optional.ofNullable(this.entriesFields.get(target.getTabList().getClass()));
            if (optionalField.isEmpty()) {
                return;
            }
            final Field entriesField = optionalField.get();
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

    protected void removePlayer(@NotNull Player target) {
        final UUID uuid = target.getUniqueId();
        final Optional<TabPlayer> tabPlayer = getTabPlayer(target.getUniqueId());
        if (tabPlayer.isEmpty()) {
            return;
        }
        final Group group = tabPlayer.get().getGroup();
        tabPlayer.get().setLoaded(false);

        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    final List<TabPlayer> list = group.getTabPlayersAsList(plugin);
                    list.forEach(player -> {
                        player.getPlayer().getTabList().removeEntry(uuid);
                        player.sendHeaderAndFooter(this);
                    });
                })
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

        final String displayNameUnformatted = plugin.getPlaceholderManager().applyPlaceholders(player, player.getGroup().format(), viewer);
        final Component displayName = formatRelationalComponent(player, viewer, displayNameUnformatted);
        player.setRelationalDisplayName(viewer.getPlayer().getUniqueId(), displayName);
        return TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(displayName)
                .latency(Math.max((int) player.getPlayer().getPing(), 0))
                .tabList(tabList)
                .build();
    }

    protected void calculateAndSetDisplayName(@NotNull TabPlayer player, @NotNull TabPlayer viewer) {
        final String withPlaceholders = plugin.getPlaceholderManager().applyPlaceholders(player, player.getGroup().format());
        final String unformatted = plugin.getPlaceholderManager().formatVelocitabPlaceholders(withPlaceholders, player, null);
        if (!plugin.getSettings().isEnableRelationalPlaceholders()) {
            final String stripped = plugin.getPlaceholderManager().stripVelocitabRelPlaceholders(unformatted);
            final Component displayName = plugin.getFormatter().format(stripped, player, plugin);
            updateEntryDisplayName(player, viewer, displayName);
            return;
        }

        final String withRelationalPlaceholders = plugin.getPlaceholderManager().formatVelocitabPlaceholders(unformatted, player, viewer);
        final Component displayName = plugin.getFormatter().format(withRelationalPlaceholders, player, viewer, plugin);
        updateEntryDisplayName(player, viewer, displayName);
    }

    protected void updateEntryDisplayName(@NotNull TabPlayer player, @NotNull TabPlayer viewer, @NotNull Component displayName) {
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

    public void updateHeaderFooter(@NotNull Group group) {
        group.getTabPlayers(plugin).forEach(p -> p.sendHeaderAndFooter(this));
    }

    // Update a player's name in the tab list and scoreboard team
    public void updatePlayer(@NotNull TabPlayer tabPlayer, boolean force) {
        if (!tabPlayer.getPlayer().isActive()) {
            removeOfflinePlayer(tabPlayer.getPlayer());
            return;
        }

        plugin.getPlaceholderManager().fetchPlaceholders(tabPlayer.getPlayer().getUniqueId(), tabPlayer.getGroup().sortingPlaceholders(), tabPlayer.getGroup());

        //to make sure that role placeholder is updated even for a backend placeholder
        plugin.getServer().getScheduler().buildTask(plugin,
                        () -> updateSorting(tabPlayer, force))
                .delay(100, TimeUnit.MILLISECONDS)
                .schedule();
    }


    public void updateSorting(@NotNull Group group) {
        final List<TabPlayer> players = group.getTabPlayersAsList(plugin);
        players.forEach(p -> updateSorting(p, false, players));
    }

    private void updateSorting(@NotNull TabPlayer tabPlayer, boolean force) {
        final List<TabPlayer> players = tabPlayer.getGroup().getTabPlayersAsList(plugin, tabPlayer);
        updateSorting(tabPlayer, force, players);
    }

    private void updateSorting(@NotNull TabPlayer tabPlayer, boolean force, @NotNull List<TabPlayer> players) {
        final String teamName = tabPlayer.getTeamName(plugin);
        if (teamName.isBlank() || !tabPlayer.getPlayer().isActive()) {
            return;
        }

        plugin.getScoreboardManager().updateRole(tabPlayer, teamName, force);
        final int order = plugin.getScoreboardManager().getPosition(teamName);
        if (order == -1) {
            plugin.log(Level.ERROR, "Failed to get position for " + tabPlayer.getPlayer().getUsername());
            return;
        }

        tabPlayer.setListOrder(order);
        recalculateSortingForPlayers(tabPlayer, players, order);
    }

    private boolean hasListOrder(TabPlayer tabPlayer) {
        return tabPlayer.getPlayer().getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_21_2);
    }

    private void updateSorting(TabPlayer tabPlayer, UUID uuid, int position) {
        tabPlayer.getPlayer().getTabList().getEntry(uuid)
                .filter(entry -> entry.getListOrder() != position)
                .ifPresent(entry -> entry.setListOrder(position));
    }

    public synchronized void recalculateSortingForPlayers(@NotNull TabPlayer tabPlayer, @NotNull List<TabPlayer> players, int order) {
        players.stream()
                .filter(this::hasListOrder)
                .forEach(p -> updateSorting(p, tabPlayer.getPlayer().getUniqueId(), order));
    }

    public void sendPlayerServerLinks(@NotNull TabPlayer player) {
        if (player.getPlayer().getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_21)) {
            return;
        }

        final List<ServerUrl> urls = plugin.getSettings().getUrlsForGroup(player.getGroup());
        final List<ServerLink> serverLinks = ServerUrl.resolve(plugin, player, urls);
        player.getPlayer().setServerLinks(serverLinks);
    }

    public void updateGroupNames(@NotNull Group group) {
        final List<TabPlayer> players = group.getTabPlayersAsList(plugin);
        if (plugin.getSettings().isEnableRelationalPlaceholders()) {
            updateRelationalGroupNames(players, group);
            return;
        }

        updateNormalGroupNames(players, group);
    }

    private void updateNormalGroupNames(List<TabPlayer> players, @NotNull Group group) {
        final String stripped = plugin.getPlaceholderManager().stripVelocitabRelPlaceholders(group.format());
        checkStrippedString(stripped, group);

        for (TabPlayer player : players) {
            final String displayName = plugin.getPlaceholderManager().applyPlaceholders(player, stripped);
            final String displayNameConditional = plugin.getPlaceholderManager().formatVelocitabPlaceholders(displayName, player, null);
            final Component displayNameComponent = formatComponent(player, displayNameConditional);
            players.forEach(viewer -> updateEntryDisplayName(player, viewer, displayNameComponent));
        }
    }

    private void updateRelationalGroupNames(@NotNull List<TabPlayer> players, @NotNull Group group) {
        for (TabPlayer p1 : players) {
            if (!p1.getPlayer().isActive() || !p1.isLoaded()) {
                return;
            }

            final boolean isVanished = plugin.getVanishManager().isVanished(p1.getPlayer().getUsername());
            final String formatPlaceholders = plugin.getPlaceholderManager().applyPlaceholders(p1, group.format());
            final String formatConditionalPlaceholders = plugin.getPlaceholderManager().formatVelocitabPlaceholders(formatPlaceholders, p1, null);

            // Handles the case where the player is not
            final String formatConditionalPlaceholdersWithoutRelational = plugin.getPlaceholderManager().stripVelocitabRelPlaceholders(formatConditionalPlaceholders);
            final Component relationalPlaceholder = formatComponent(p1, formatConditionalPlaceholdersWithoutRelational);

            for (TabPlayer player : players) {
                if (isVanished && !plugin.getVanishManager().canSee(player.getPlayer().getUsername(), p1.getPlayer().getUsername())) {
                    return;
                }

                if (!player.getPlayer().isActive() || !player.isLoaded()) {
                    return;
                }

                if (!player.getPlayer().hasPermission(RELATIONAL_PERMISSION)) {
                    updateEntryDisplayName(p1, player, relationalPlaceholder);
                    continue;
                }

                final String withPlaceholders = plugin.getPlaceholderManager().applyViewerPlaceholders(player, formatConditionalPlaceholders);
                final String unformatted = plugin.getPlaceholderManager().formatVelocitabPlaceholders(withPlaceholders, p1, player);

                final Component displayName = formatRelationalComponent(p1, player, unformatted);
                updateEntryDisplayName(p1, player, displayName);
            }
        }
    }

    public void updateDisplayName(@NotNull TabPlayer tabPlayer) {
        if (plugin.getSettings().isEnableRelationalPlaceholders()) {
            updateRelationalDisplayName(tabPlayer);
            return;
        }

        updateNormalDisplayName(tabPlayer);
    }

    private void updateNormalDisplayName(@NotNull TabPlayer tabPlayer) {
        final Group group = tabPlayer.getGroup();
        final String stripped = plugin.getPlaceholderManager().stripVelocitabRelPlaceholders(group.format());
        checkStrippedString(stripped, group);

        final List<TabPlayer> players = group.getTabPlayersAsList(plugin, tabPlayer);
        players.forEach(player -> {
            final String displayName = plugin.getPlaceholderManager().applyPlaceholders(player, stripped);
            final String displayNameConditional = plugin.getPlaceholderManager().formatVelocitabPlaceholders(displayName, player, null);
            final Component displayNameComponent = formatComponent(player, displayNameConditional);
            updateEntryDisplayName(player, tabPlayer, displayNameComponent);
        });
    }

    private void updateRelationalDisplayName(@NotNull TabPlayer tabPlayer) {
        final Group group = tabPlayer.getGroup();
        final String stripped = plugin.getPlaceholderManager().stripVelocitabRelPlaceholders(group.format());
        checkStrippedString(stripped, group);

        final List<TabPlayer> players = group.getTabPlayersAsList(plugin, tabPlayer);
        players.forEach(player -> {
            final String displayName = plugin.getPlaceholderManager().applyPlaceholders(player, stripped);
            final String displayNameConditional = plugin.getPlaceholderManager().formatVelocitabPlaceholders(displayName, player, null);
            final Component displayNameComponent = formatRelationalComponent(player, tabPlayer, displayNameConditional);
            updateEntryDisplayName(player, tabPlayer, displayNameComponent);
        });
    }

    private void checkStrippedString(@NotNull String text, @NotNull Group group) {
        if (plugin.getSettings().isDebug() && text.length() != group.format().length()) {
            plugin.getLogger().warn("Found relational placeholder in group {} format even though relational placeholders are disabled", group.name());
        }
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

    public void checkCorrectDisplayNames() {
        players.values().forEach(this::checkCorrectDisplayName);
    }

    public void ensureDisplayNameTask() {
        plugin.getServer().getScheduler()
                .buildTask(plugin, this::checkCorrectDisplayNames)
                .delay(1, TimeUnit.SECONDS)
                .repeat(5, TimeUnit.SECONDS)
                .schedule();
    }

    // Get the component for the TAB list header
    public Component getHeader(@NotNull TabPlayer player) {
        final String header = player.getGroup().getHeader(player.getHeaderIndex());
        final String replaced = plugin.getPlaceholderManager().applyPlaceholders(player, header);

        return plugin.getFormatter().format(replaced, player, plugin);
    }

    // Get the component for the TAB list footer
    public Component getFooter(@NotNull TabPlayer player) {
        final String footer = player.getGroup().getFooter(player.getFooterIndex());
        final String replaced = plugin.getPlaceholderManager().applyPlaceholders(player, footer);

        return plugin.getFormatter().format(replaced, player, plugin);
    }

    /**
     * Update the TAB list for all players when a plugin or proxy reload is performed
     */
    public void reloadUpdate() {
        taskManager.cancelAllTasks();
        plugin.getPlaceholderManager().reload();
        plugin.getPlaceholderManager().preparePlaceholdersReplacements();
        plugin.getTabGroupsManager().getGroups().forEach(g -> {
            plugin.getPlaceholderManager().fetchPlaceholders(g);
            taskManager.updatePeriodically(g);
        });

        if (players.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
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
            plugin.getTabGroupsManager().getGroups().forEach(this::updateGroupNames);
        }).delay(500, TimeUnit.MILLISECONDS).schedule();
    }

    @NotNull
    public Optional<Group> getGroup(@NotNull String serverName) {
        return plugin.getTabGroupsManager().getGroupFromServer(serverName, plugin);
    }

    @NotNull
    public Group getGroupOrDefault(@NotNull Player player) {
        final Optional<Group> group = getGroup(player.getCurrentServer().map(ServerConnection::getServerInfo).map(ServerInfo::getName).orElse(""));
        return group.orElse(plugin.getTabGroupsManager().getGroup("default").orElseThrow());
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
}
