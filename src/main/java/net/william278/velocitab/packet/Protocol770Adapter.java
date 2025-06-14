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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Adapter for handling the UpdateTeamsPacket for Minecraft 1.21.5
 */
public class Protocol770Adapter extends Protocol765Adapter {

    public Protocol770Adapter(@NotNull Velocitab plugin) {
        super(plugin, Set.of(
                ProtocolVersion.MINECRAFT_1_21_5
//                ,
//                ProtocolVersion.MINECRAFT_1_21_6
        ));
    }

    @Override
    public void decode(@NotNull ByteBuf byteBuf, @NotNull UpdateTeamsPacket packet, @NotNull ProtocolVersion protocolVersion) {
        packet.teamName(ProtocolUtils.readString(byteBuf));
        UpdateTeamsPacket.UpdateMode mode = UpdateTeamsPacket.UpdateMode.byId(byteBuf.readByte());
        packet.mode(mode);
        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            packet.displayName(readComponent(byteBuf));
            packet.friendlyFlags(UpdateTeamsPacket.FriendlyFlag.fromBitMask(byteBuf.readByte()));
            packet.nametagVisibility(UpdateTeamsPacket.NametagVisibility.byOrdinal(ProtocolUtils.readVarInt(byteBuf)));
            packet.collisionRule(UpdateTeamsPacket.CollisionRule.byOrdinal(ProtocolUtils.readVarInt(byteBuf)));
            packet.color(byteBuf.readByte());
            packet.prefix(readComponent(byteBuf));
            packet.suffix(readComponent(byteBuf));
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            int count = ProtocolUtils.readVarInt(byteBuf);
            List<String> entities = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                entities.add(ProtocolUtils.readString(byteBuf));
            }
            packet.entities(entities);
        }
    }

    @Override
    public void encode(@NotNull ByteBuf byteBuf, @NotNull UpdateTeamsPacket packet, @NotNull ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(byteBuf, packet.teamName());
        UpdateTeamsPacket.UpdateMode mode = packet.mode();
        byteBuf.writeByte(mode.id());
        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            writeComponent(byteBuf, packet.displayName());
            byteBuf.writeByte(UpdateTeamsPacket.FriendlyFlag.toBitMask(packet.friendlyFlags()));
            ProtocolUtils.writeVarInt(byteBuf, packet.nametagVisibility().ordinal());
            ProtocolUtils.writeVarInt(byteBuf, packet.collisionRule().ordinal());
            byteBuf.writeByte(packet.color());
            writeComponent(byteBuf, packet.prefix());
            writeComponent(byteBuf, packet.suffix());
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            List<String> entities = packet.entities();
            ProtocolUtils.writeVarInt(byteBuf, entities != null ? entities.size() : 0);
            for (String entity : entities != null ? entities : new ArrayList<String>()) {
                ProtocolUtils.writeString(byteBuf, entity);
            }
        }
    }

}
