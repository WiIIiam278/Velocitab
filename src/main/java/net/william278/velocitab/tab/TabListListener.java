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

import com.google.common.collect.Sets;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The TabListListener class is responsible for handling events related to the player tab list.
 */
@SuppressWarnings("unused")
public class TabListListener {

    private final Velocitab plugin;
    private final PlayerTabList tabList;

    // Set of UUIDs of users who just left the game - fixes packet delay problem on Minecraft 1.8.x
    private final Set<UUID> justQuit;

    public TabListListener(@NotNull Velocitab plugin, @NotNull PlayerTabList tabList) {
        this.plugin = plugin;
        this.tabList = tabList;
        this.justQuit = Sets.newConcurrentHashSet();
    }

    @Subscribe
    public void onKick(@NotNull KickedFromServerEvent event) {
        event.getPlayer().getTabList().getEntries().stream()
                .filter(entry -> entry.getProfile() != null && !entry.getProfile().getId().equals(event.getPlayer().getUniqueId()))
                .forEach(entry -> event.getPlayer().getTabList().removeEntry(entry.getProfile().getId()));
        event.getPlayer().getTabList().clearHeaderAndFooter();

        if (event.getResult() instanceof KickedFromServerEvent.DisconnectPlayer) {
            tabList.removePlayer(event.getPlayer());
        } else if (event.getResult() instanceof KickedFromServerEvent.RedirectPlayer redirectPlayer) {
            tabList.removePlayer(event.getPlayer(), redirectPlayer.getServer());
        } else if (event.getResult() instanceof KickedFromServerEvent.Notify notify) {
            return;
        }

        event.getPlayer().getTabList().removeEntry(event.getPlayer().getUniqueId());
        event.getPlayer().getTabList().clearHeaderAndFooter();
        justQuit.add(event.getPlayer().getUniqueId());

        plugin.getServer().getScheduler().buildTask(plugin,
                        () -> justQuit.remove(event.getPlayer().getUniqueId()))
                .delay(300, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe(priority = Short.MIN_VALUE)
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final Player joined = event.getPlayer();
        final String serverName = joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("");

        final Optional<TabPlayer> previousTabPlayer = tabList.getTabPlayer(joined);
        final Optional<Group> previousGroup = previousTabPlayer
                .map(TabPlayer::getGroup);

        // Get the group the player should now be in
        final @NotNull Optional<Group> groupOptional = tabList.getGroup(serverName);
        final boolean isDefault = groupOptional.map(g -> g.isDefault(plugin)).orElse(true);

        if (joined.getCurrentServer().isPresent()) {
            final RegisteredServer server = joined.getCurrentServer().get().getServer();
            final Set<UUID> players = server.getPlayersConnected().stream()
                    .map(Player::getUniqueId).collect(Collectors.toSet());
            List.copyOf(event.getPlayer().getTabList().getEntries()).stream()
                    .filter(entry -> !players.contains(entry.getProfile().getId()))
                    .forEach(entry -> event.getPlayer().getTabList().removeEntry(entry.getProfile().getId()));
        }

        // Removes cached relational data of the joined player from all other players
        plugin.getTabList().clearCachedData(joined);
        plugin.getPlaceholderManager().clearPlaceholders(joined.getUniqueId());

        // Mark the previous tab player as unloaded
        previousTabPlayer.ifPresent(player -> player.setLoaded(false));

        // If the player was in a group and the new group is different or not set, remove the old entry
        if (!plugin.getSettings().isShowAllPlayersFromAllGroups() && previousGroup.isPresent()
                && ((groupOptional.isPresent() && !previousGroup.get().equals(groupOptional.get())) || groupOptional.isEmpty())
        ) {
            tabList.getPlayers().remove(joined.getUniqueId());
            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                        tabList.removeOldEntry(previousGroup.get(), joined.getUniqueId());
                    })
                    .delay(100, TimeUnit.MILLISECONDS)
                    .schedule();
        }

        // If the server is not in a group, use fallback.
        // If fallback is disabled, permit the player to switch excluded servers without a header or footer override
        if (isDefault && !plugin.getSettings().isFallbackEnabled() && !groupOptional.map(g -> g.containsServer(plugin, serverName)).orElse(false)) {
            if (previousTabPlayer.isEmpty()) {
                return;
            }

            if (event.getPreviousServer() == null) {
                return;
            }

            final Component header = previousTabPlayer.get().getLastHeader();
            final Component footer = previousTabPlayer.get().getLastFooter();

            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                final Component currentHeader = joined.getPlayerListHeader();
                final Component currentFooter = joined.getPlayerListFooter();
                if ((header.equals(currentHeader) && footer.equals(currentFooter)) ||
                        (currentHeader.equals(Component.empty()) && currentFooter.equals(Component.empty()))
                ) {
                    joined.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
                    joined.getCurrentServer().ifPresent(serverConnection -> serverConnection.getServer().getPlayersConnected().forEach(player ->
                            player.getTabList().getEntry(joined.getUniqueId())
                                    .ifPresent(entry -> entry.setDisplayName(Component.text(joined.getUsername())))));
                }
            }).delay(500, TimeUnit.MILLISECONDS).schedule();

            tabList.getPlayers().remove(event.getPlayer().getUniqueId());
            return;
        }

        if (groupOptional.isEmpty()) {
            return;
        }

        final Group group = groupOptional.get();
        plugin.getScoreboardManager().resetCache(joined, group);

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            plugin.getPlaceholderManager().unblockPlayer(joined.getUniqueId());
        }).delay(10, TimeUnit.MILLISECONDS).schedule();

        tabList.loadPlayer(joined, group, justQuit.contains(joined.getUniqueId()) ? 400 : 500);
    }

    @SuppressWarnings("deprecation")
    @Subscribe(order = PostOrder.CUSTOM, priority = Short.MIN_VALUE)
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        if (event.getLoginStatus() == DisconnectEvent.LoginStatus.CONFLICTING_LOGIN) {
            return;
        }
//        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
//            checkDelayedDisconnect(event);
//            return;
//        }

        // Remove the player from the tab list of all other players
        tabList.removePlayer(event.getPlayer());
        plugin.getPlaceholderManager().clearPlaceholders(event.getPlayer().getUniqueId());
        plugin.getPlaceholderManager().unblockPlayer(event.getPlayer().getUniqueId());
    }

    private void checkDelayedDisconnect(@NotNull DisconnectEvent event) {
        final Player player = event.getPlayer();
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            final Optional<Player> actualPlayer = plugin.getServer().getPlayer(player.getUniqueId());
            if (actualPlayer.isPresent() && !actualPlayer.get().equals(player)) {
                return;
            }
            if (player.getCurrentServer().isPresent()) {
                return;
            }

            tabList.removeOfflinePlayer(player);
            tabList.removeTabListUUID(event.getPlayer().getUniqueId());
        }).delay(750, TimeUnit.MILLISECONDS).schedule();
    }

    @Subscribe
    public void proxyReload(@NotNull ProxyReloadEvent event) {
        plugin.loadConfigs();
        tabList.reloadUpdate();
        plugin.log("Velocitab has been reloaded!");
    }

}
