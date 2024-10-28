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
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Adapter for handling the UpdateTeamsPacket for Minecraft 1.8.x
 */
@SuppressWarnings("DuplicatedCode")
public class Protocol48Adapter extends TeamsPacketAdapter {

    private final LegacyComponentSerializer serializer;

    public Protocol48Adapter(@NotNull Velocitab plugin) {
        super(plugin, Set.of(ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_12_2));
        serializer = LegacyComponentSerializer.legacySection();
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
            packet.prefix(readComponent(byteBuf));
            packet.suffix(readComponent(byteBuf));
            packet.friendlyFlags(UpdateTeamsPacket.FriendlyFlag.fromBitMask(byteBuf.readByte()));
            packet.nametagVisibility(UpdateTeamsPacket.NametagVisibility.byId(ProtocolUtils.readString(byteBuf)));
            if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_12_2) >= 0) {
                packet.collisionRule(UpdateTeamsPacket.CollisionRule.byId(ProtocolUtils.readString(byteBuf)));
            }
            packet.color(byteBuf.readByte());
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
        ProtocolUtils.writeString(byteBuf, shrinkString(packet.teamName()));
        UpdateTeamsPacket.UpdateMode mode = packet.mode();
        byteBuf.writeByte(mode.id());
        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            writeComponent(byteBuf, packet.displayName());
            writeComponent(byteBuf, packet.prefix());
            writeComponent(byteBuf, packet.suffix());
            byteBuf.writeByte(UpdateTeamsPacket.FriendlyFlag.toBitMask(packet.friendlyFlags()));
            ProtocolUtils.writeString(byteBuf, packet.nametagVisibility().id());
            if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_12_2) >= 0) {
                ProtocolUtils.writeString(byteBuf, packet.collisionRule().id());
            }
            byteBuf.writeByte(packet.color());
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            List<String> entities = packet.entities();
            ProtocolUtils.writeVarInt(byteBuf, entities != null ? entities.size() : 0);
            for (String entity : entities != null ? entities : new ArrayList<String>()) {
                ProtocolUtils.writeString(byteBuf, entity);
            }
        }
    }

    /**
     * Returns a shortened version of the given string, with a maximum length of 16 characters.
     * This is used to ensure that the team name, display name, prefix and suffix are not too long for the client.
     *
     * @param string the string to be shortened
     * @return the shortened string
     */
    @NotNull
    private String shrinkString(@NotNull String string) {
        return string.substring(0, Math.min(string.length(), 16));
    }

    protected void writeComponent(@NotNull ByteBuf buf, @NotNull Component component) {
        ProtocolUtils.writeString(buf, shrinkString(serializer.serialize(component)));
    }

    @NotNull
    protected Component readComponent(@NotNull ByteBuf buf) {
        return serializer.deserialize(ProtocolUtils.readString(buf));
    }
}
