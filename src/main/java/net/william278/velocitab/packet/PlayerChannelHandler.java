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
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import net.william278.velocitab.Velocitab;
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
        if (!(msg instanceof final UpsertPlayerInfoPacket minecraftPacket)) {
            super.write(ctx, msg, promise);
            return;
        }

        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
        if (tabPlayer.isEmpty()) {
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

        plugin.getPacketEventManager().handleEntry(minecraftPacket, player);
        super.write(ctx, msg, promise);
    }

    private void forceGameMode(@NotNull List<UpsertPlayerInfoPacket.Entry> entries) {
        entries.stream()
                .filter(entry -> entry.getProfileId() != null && entry.getGameMode() == 3 && !entry.getProfileId().equals(player.getUniqueId()))
                .forEach(entry -> entry.setGameMode(0));
    }
}
