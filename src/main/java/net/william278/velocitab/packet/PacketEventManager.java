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

package net.william278.velocitab.packet;

import com.google.common.collect.Sets;
import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.channel.Channel;
import lombok.Getter;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PacketEventManager {

    private static final String KEY = "velocitab";
    private static final String CITIZENS_PREFIX = "CIT";

    private final Velocitab plugin;
    @Getter
    private final Set<UUID> velocitabEntries;

    public PacketEventManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.velocitabEntries = Sets.newConcurrentHashSet();
        this.loadPlayers();
        this.loadListeners();
    }

    private void loadPlayers() {
        plugin.getServer().getAllPlayers().forEach(this::injectPlayer);
    }

    private void loadListeners() {
        plugin.getServer().getEventManager().register(plugin, PostLoginEvent.class,
                (AwaitingEventExecutor<PostLoginEvent>) postLoginEvent -> EventTask.withContinuation(continuation -> {
                    injectPlayer(postLoginEvent.getPlayer());
                    continuation.resume();
                }));

        plugin.getServer().getEventManager().register(plugin, DisconnectEvent.class,
                (AwaitingEventExecutor<DisconnectEvent>) disconnectEvent ->
                        disconnectEvent.getLoginStatus() == DisconnectEvent.LoginStatus.CONFLICTING_LOGIN
                                ? null
                                : EventTask.async(() -> removePlayer(disconnectEvent.getPlayer())));
    }

    public void injectPlayer(@NotNull Player player) {
        final PlayerChannelHandler handler = new PlayerChannelHandler(plugin, player);
        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        removePlayer(player);
        connectedPlayer.getConnection()
                .getChannel()
                .pipeline()
                .addBefore(Connections.HANDLER, KEY, handler);
    }

    public void removePlayer(@NotNull Player player) {
        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        final Channel channel = connectedPlayer.getConnection().getChannel();
        if (channel.pipeline().get(KEY) != null) {
            channel.pipeline().remove(KEY);
        }
    }

    protected void handleEntry(@NotNull UpsertPlayerInfoPacket packet, @NotNull Player player) {
        final List<TabPlayer> toUpdate = packet.getEntries().stream()
                .filter(entry -> entry.getProfile() != null)
                .filter(entry -> !entry.getProfile().getName().startsWith(CITIZENS_PREFIX))
                .filter(entry -> velocitabEntries.stream().noneMatch(uuid -> uuid.equals(entry.getProfile().getId())))
                .map(entry -> entry.getProfile().getId())
                .map(id -> plugin.getTabList().getTabPlayer(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(TabPlayer::isLoaded)
                .toList();

        if (toUpdate.isEmpty()) {
            return;
        }

        toUpdate.forEach(tabPlayer -> packet.getEntries().stream()
                .filter(entry -> entry.getProfileId().equals(tabPlayer.getPlayer().getUniqueId()))
                .findFirst()
                .ifPresent(entry -> entry.setDisplayName(
                        new ComponentHolder(player.getProtocolVersion(), tabPlayer.getLastDisplayname()))));
    }

}
