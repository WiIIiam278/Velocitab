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

import com.google.common.collect.Maps;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.packet.UpdateTeamsPacket;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.util.DebugSystem;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class PluginMessageAPI {

    private static final Map<MinecraftChannelIdentifier, PluginMessageRequest> CHANNELS = Maps.newHashMap();

    private final Velocitab plugin;
    private final Map<String, MinecraftChannelIdentifier> channels;

    public PluginMessageAPI(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.channels = Maps.newHashMap();
    }

    public void registerChannel() {
        Arrays.stream(PluginMessageRequest.values())
                .forEach(request -> {
                    final String requestName = request.name().toLowerCase(Locale.ENGLISH);
                    final String channelName = "velocitab:" + requestName;
                    final MinecraftChannelIdentifier channel = MinecraftChannelIdentifier.from(channelName);
                    channels.put(channelName, channel);
                    plugin.getServer().getChannelRegistrar().register(channel);
                    CHANNELS.put(channel, request);
                });
        plugin.getServer().getEventManager().register(plugin, PluginMessageEvent.class, this::onPluginMessage);
    }

    public void unregisterChannel() {
        channels.forEach((name, channel) -> plugin.getServer().getChannelRegistrar().unregister(channel));
    }

    private void onPluginMessage(@NotNull PluginMessageEvent pluginMessageEvent) {
        final Optional<MinecraftChannelIdentifier> channel = Optional.ofNullable(channels.get(pluginMessageEvent.getIdentifier().getId()));
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
                final UpdateTeamsPacket.TeamColor color = UpdateTeamsPacket.TeamColor.getColor(colorChar);
                DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Team color for " + tabPlayer.getPlayer().getUsername() + " is " + color + " (" + arg + ")");
                tabPlayer.setTeamColor(color);
                plugin.getTabList().updatePlayer(tabPlayer, true);
            }
        }
    }

    private enum PluginMessageRequest {
        UPDATE_CUSTOM_NAME,
        UPDATE_TEAM_COLOR;

        public static Optional<PluginMessageRequest> get(@NotNull MinecraftChannelIdentifier channelIdentifier) {
            return Optional.ofNullable(CHANNELS.get(channelIdentifier));
        }
    }

}
