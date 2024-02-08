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

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.RequiredArgsConstructor;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.packet.UpdateTeamsPacket;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public class PluginMessageAPI {

    private static final MinecraftChannelIdentifier VELOCITAB_CHANNEL = MinecraftChannelIdentifier.from("velocitab:main");
    private static final String SPLITERATOR = ":::";
    private final Velocitab plugin;

    public void registerChannel() {
        plugin.getServer().getChannelRegistrar().register(VELOCITAB_CHANNEL);
        plugin.getServer().getEventManager().register(plugin, PluginMessageEvent.class, this::onPluginMessage);
    }

    public void unregisterChannel() {
        plugin.getServer().getChannelRegistrar().unregister(VELOCITAB_CHANNEL);
    }

    private void onPluginMessage(@NotNull PluginMessageEvent pluginMessageEvent) {
        if (!pluginMessageEvent.getIdentifier().equals(VELOCITAB_CHANNEL)) {
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

        final String data = new String(pluginMessageEvent.getData());
        final String[] split = data.split(SPLITERATOR, 2);
        if (split.length != 2) {
            return;
        }

        final Optional<APIRequest> request = APIRequest.get(split[0]);
        if (request.isEmpty()) {
            return;
        }

        handleAPIRequest(tabPlayer, request.get(), split[1]);
    }

    private void handleAPIRequest(@NotNull TabPlayer tabPlayer, @NotNull APIRequest request, String arg) {
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

    private enum APIRequest {
        UPDATE_CUSTOM_NAME,
        UPDATE_TEAM_COLOR;

        public static Optional<APIRequest> get(String name) {
            return Arrays.stream(values())
                    .filter(request -> request.name().equalsIgnoreCase(name))
                    .findFirst();
        }
    }

}
