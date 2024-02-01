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
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The TabListListener class is responsible for handling events related to the player tab list.
 */
@SuppressWarnings("unused")
public class TabListListener {

    private final Velocitab plugin;
    private final PlayerTabList tabList;

    public TabListListener(@NotNull Velocitab plugin, @NotNull PlayerTabList tabList) {
        this.plugin = plugin;
        this.tabList = tabList;
    }

    @Subscribe
    public void onKick(@NotNull KickedFromServerEvent event) {
        event.getPlayer().getTabList().clearAll();
        event.getPlayer().getTabList().clearHeaderAndFooter();

        if (event.getResult() instanceof KickedFromServerEvent.DisconnectPlayer) {
            tabList.removePlayer(event.getPlayer());
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final Player joined = event.getPlayer();

        final String serverName = joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("");
        final Group group = tabList.getGroup(serverName);
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(joined, group));
        final boolean isDefault = !group.servers().contains(serverName);

        // If the server is not in a group, use fallback.
        // If fallback is disabled, permit the player to switch excluded servers without a header or footer override
        if (isDefault && !plugin.getSettings().isFallbackEnabled()) {
            final Optional<TabPlayer> tabPlayer = tabList.getTabPlayer(joined);
            if (tabPlayer.isEmpty()) {
                return;
            }
            final Component header = tabPlayer.get().getLastHeader();
            final Component footer = tabPlayer.get().getLastFooter();
            final Component displayName = tabPlayer.get().getLastDisplayName();

            plugin.getServer().getScheduler().buildTask(plugin, () -> {
                if (header.equals(event.getPlayer().getPlayerListHeader()) && footer.equals(event.getPlayer().getPlayerListFooter())) {
                    event.getPlayer().sendPlayerListHeaderAndFooter(header, footer);
                    event.getPlayer().getCurrentServer().ifPresent(serverConnection ->
                            serverConnection.getServer().getPlayersConnected().forEach(player ->
                                    player.getTabList().getEntry(joined.getUniqueId()).ifPresent(entry -> {
                                        if (entry.getDisplayNameComponent().isPresent() && entry.getDisplayNameComponent().get().equals(displayName)) {
                                            entry.setDisplayName(Component.text(joined.getUsername()));
                                        }
                                    })));
                }
            }).delay(500, TimeUnit.MILLISECONDS).schedule();

            tabList.getPlayers().remove(event.getPlayer().getUniqueId());
            return;
        }

        tabList.joinPlayer(joined, group);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
            return;
        }

        // Remove the player from the tracking list, Print warning if player was not removed
        final UUID uuid = event.getPlayer().getUniqueId();

        // Remove the player from the tab list of all other players
        tabList.removePlayer(event.getPlayer());
    }

    @Subscribe
    public void proxyReload(@NotNull ProxyReloadEvent event) {
        plugin.loadConfigs();
        tabList.reloadUpdate();
        plugin.log("Velocitab has been reloaded!");
    }

}
