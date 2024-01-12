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
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;
import net.william278.velocitab.Velocitab;

@RequiredArgsConstructor
public class PlayerChannelHandler extends ChannelDuplexHandler {

    private final Velocitab plugin;
    private final Player player;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof final UpsertPlayerInfo minecraftPacket)) {
            super.write(ctx, msg, promise);
            return;
        }

        if(!minecraftPacket.containsAction(UpsertPlayerInfo.Action.ADD_PLAYER) && !minecraftPacket.containsAction(UpsertPlayerInfo.Action.UPDATE_LISTED)) {
            super.write(ctx, msg, promise);
            return;
        }

        if (minecraftPacket.getEntries().stream().allMatch(entry -> entry.getProfile() != null && entry.getProfile().getName().startsWith("CIT"))) {
            super.write(ctx, msg, promise);
            return;
        }

        plugin.getChannelManager().handleEntry(minecraftPacket, player);
        super.write(ctx, msg, promise);
    }
}
