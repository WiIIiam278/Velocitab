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

package net.william278.velocitab.api;

import com.google.common.collect.Sets;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.packet.UpdateTeamsPacket;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class PluginMessageAPI {

    private final Velocitab plugin;
    private final Set<MinecraftChannelIdentifier> channels;

    public PluginMessageAPI(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.channels = Sets.newConcurrentHashSet();
    }

    public void registerChannel() {
        Arrays.stream(PluginMessageRequest.values())
                .map(PluginMessageRequest::name)
                .forEach(request -> {
                    final String channelName = request.toLowerCase();
                    final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from("velocitab:" + channelName);
                    channels.add(channel);
                    plugin.getServer().getChannelRegistrar().register(channel);
                });
        plugin.getServer().getEventManager().register(plugin, PluginMessageEvent.class, this::onPluginMessage);
    }

    public void unregisterChannel() {
        channels.forEach(channel -> plugin.getServer().getChannelRegistrar().unregister(channel));
    }

    private void onPluginMessage(@NotNull PluginMessageEvent pluginMessageEvent) {
        final Optional<MinecraftChannelIdentifier> channel = channels.stream()
                .filter(identifier -> identifier.equals(pluginMessageEvent.getIdentifier()))
                .findFirst();
        if (channel.isEmpty()) {
            return;
        }
        if (!(pluginMessageEvent.getSource() instanceof ServerConnection serverConnection)) {
            return;
        }

        final Player player = serverConnection.getPlayer();
        final Optional<TabPlayer> optionalTabPlayer = plugin.getTabList().getTabPlayer(player);
        if (optionalTabPlayer.isEmpty()) {
            return;
        }

        final TabPlayer tabPlayer = optionalTabPlayer.get();
        if (!tabPlayer.isLoaded()) {
            return;
        }

        final Optional<PluginMessageRequest> request = PluginMessageRequest.get(channel.get());
        if (request.isEmpty()) {
            return;
        }

        final String data = new String(pluginMessageEvent.getData());
        handleAPIRequest(tabPlayer, request.get(), data);
    }

    private void handleAPIRequest(@NotNull TabPlayer tabPlayer, @NotNull PluginMessageRequest request, @NotNull String arg) {
        switch (request) {
            case UPDATE_CUSTOM_NAME -> {
                tabPlayer.setCustomName(arg);
                plugin.getTabList().updatePlayer(tabPlayer, true);
            }
            case UPDATE_TEAM_COLOR -> {
                final String clean = arg.replaceAll("&", "").replaceAll("§", "");
                if (clean.isEmpty()) {
                    return;
                }
                final char colorChar = clean.charAt(0);
                final Optional<UpdateTeamsPacket.TeamColor> color = Arrays.stream(UpdateTeamsPacket.TeamColor.values())
                        .filter(teamColor -> teamColor.colorChar() == colorChar)
                        .findFirst();
                color.ifPresent(teamColor -> {
                    tabPlayer.setTeamColor(teamColor);
                    plugin.getTabList().updatePlayer(tabPlayer, true);
                });
            }
        }
    }

    private enum PluginMessageRequest {
        UPDATE_CUSTOM_NAME,
        UPDATE_TEAM_COLOR;

        public static Optional<PluginMessageRequest> get(@NotNull MinecraftChannelIdentifier channelIdentifier) {
            return Arrays.stream(values())
                    .filter(request -> request.name().equalsIgnoreCase(channelIdentifier.getName()))
                    .findFirst();
        }
    }

}
