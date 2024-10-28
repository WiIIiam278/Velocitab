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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class PlayerChannelHandler extends ChannelDuplexHandler {

    private final Velocitab plugin;
    private final Player player;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof final UpdateTeamsPacket updateTeamsPacket && plugin.getSettings().isSendScoreboardPackets()) {
            final ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
            if (!scoreboardManager.handleTeams()) {
                super.write(ctx, msg, promise);
                return;
            }

            if (updateTeamsPacket.isRemoveTeam()) {
                super.write(ctx, msg, promise);
                return;
            }

            if (scoreboardManager.isInternalTeam(updateTeamsPacket.teamName())) {
                super.write(ctx, msg, promise);
                return;
            }

            if (!updateTeamsPacket.hasEntities()) {
                super.write(ctx, msg, promise);
                return;
            }

            if (updateTeamsPacket.entities().stream().noneMatch(entity -> plugin.getServer().getPlayer(entity).isPresent())) {
                super.write(ctx, msg, promise);
                return;
            }

            // Cancel packet if the backend is trying to send a team packet with an online player.
            // This is to prevent conflicts with Velocitab teams.
            plugin.getLogger().warn("Cancelled team \"{}\" packet from backend for player {}. " +
                                    "We suggest disabling \"send_scoreboard_packets\" in Velocitab's config.yml file, " +
                                    "but note this will disable TAB sorting",
                    updateTeamsPacket.teamName(), player.getUsername());
            return;
        }
        if (!(msg instanceof final UpsertPlayerInfoPacket minecraftPacket)) {
            super.write(ctx, msg, promise);
            return;
        }

        try {
            final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
            if (tabPlayer.isEmpty() && !isFutureTabPlayer()) {
                super.write(ctx, msg, promise);
                return;
            }

            if (plugin.getSettings().isRemoveSpectatorEffect() && minecraftPacket.containsAction(UpsertPlayerInfoPacket.Action.UPDATE_GAME_MODE)) {
                forceGameMode(minecraftPacket.getEntries());
            }

            //fix for duplicate entries
            if (minecraftPacket.containsAction(UpsertPlayerInfoPacket.Action.ADD_PLAYER)) {
                minecraftPacket.getEntries().stream()
                        .filter(entry -> entry.getProfile() != null && !entry.getProfile().getId().equals(entry.getProfileId()))
                        .forEach(entry -> entry.setListed(false));
            }

            if (!minecraftPacket.containsAction(UpsertPlayerInfoPacket.Action.ADD_PLAYER) && !minecraftPacket.containsAction(UpsertPlayerInfoPacket.Action.UPDATE_LISTED)) {
                super.write(ctx, msg, promise);
                return;
            }

            if (minecraftPacket.getEntries().stream().allMatch(entry -> entry.getProfile() != null && entry.getProfile().getName().startsWith("CIT"))) {
                super.write(ctx, msg, promise);
                return;
            }

            super.write(ctx, msg, promise);
        } catch (Exception e) {
            plugin.getLogger().error("An error occurred while handling a packet", e);
            super.write(ctx, msg, promise);
        }
    }

    private void forceGameMode(@NotNull List<UpsertPlayerInfoPacket.Entry> entries) {
        entries.stream()
                .filter(entry -> entry.getProfileId() != null && entry.getGameMode() == 3 && !entry.getProfileId().equals(player.getUniqueId()))
                .forEach(entry -> entry.setGameMode(0));
    }

    private boolean isFutureTabPlayer() {
        final String serverName = player.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("");

        final Optional<Group> groupOptional = plugin.getTabList().getGroup(serverName);
        return groupOptional.isPresent();
    }
}
