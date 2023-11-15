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
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Formatter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Adapter for handling the UpdateTeamsPacket for Minecraft 1.13.2+
 */
@SuppressWarnings("DuplicatedCode")
public class Protocol403Adapter extends TeamsPacketAdapter {

    public Protocol403Adapter(@NotNull Velocitab plugin) {
        super(plugin, Set.of(ProtocolVersion.MINECRAFT_1_13_2,
                ProtocolVersion.MINECRAFT_1_14,
                ProtocolVersion.MINECRAFT_1_14_1,
                ProtocolVersion.MINECRAFT_1_14_2,
                ProtocolVersion.MINECRAFT_1_14_3,
                ProtocolVersion.MINECRAFT_1_14_4,
                ProtocolVersion.MINECRAFT_1_15,
                ProtocolVersion.MINECRAFT_1_15_1,
                ProtocolVersion.MINECRAFT_1_15_2,
                ProtocolVersion.MINECRAFT_1_16,
                ProtocolVersion.MINECRAFT_1_16_1,
                ProtocolVersion.MINECRAFT_1_16_2,
                ProtocolVersion.MINECRAFT_1_16_3,
                ProtocolVersion.MINECRAFT_1_16_4,
                ProtocolVersion.MINECRAFT_1_17,
                ProtocolVersion.MINECRAFT_1_17_1,
                ProtocolVersion.MINECRAFT_1_18,
                //ProtocolVersion.MINECRAFT_1_18_1,
                ProtocolVersion.MINECRAFT_1_18_2,
                ProtocolVersion.MINECRAFT_1_19,
                ProtocolVersion.MINECRAFT_1_19_1,
                //ProtocolVersion.MINECRAFT_1_19_2,
                ProtocolVersion.MINECRAFT_1_19_3,
                ProtocolVersion.MINECRAFT_1_19_4,
                ProtocolVersion.MINECRAFT_1_20,
                ProtocolVersion.MINECRAFT_1_20_2
        ));
    }

    @Override
    public void encode(@NotNull ByteBuf byteBuf, @NotNull UpdateTeamsPacket packet) {
        ProtocolUtils.writeString(byteBuf, packet.teamName());
        UpdateTeamsPacket.UpdateMode mode = packet.mode();
        byteBuf.writeByte(mode.id());
        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            ProtocolUtils.writeString(byteBuf, getChatString(packet.displayName()));
            byteBuf.writeByte(UpdateTeamsPacket.FriendlyFlag.toBitMask(packet.friendlyFlags()));
            ProtocolUtils.writeString(byteBuf, packet.nametagVisibility().id());
            ProtocolUtils.writeString(byteBuf, packet.collisionRule().id());
            byteBuf.writeByte(packet.color());
            ProtocolUtils.writeString(byteBuf, getRGBChat(packet.prefix()));
            ProtocolUtils.writeString(byteBuf, getRGBChat(packet.suffix()));
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            List<String> entities = packet.entities();
            ProtocolUtils.writeVarInt(byteBuf, entities != null ? entities.size() : 0);
            for (String entity : entities != null ? entities : new ArrayList<String>()) {
                ProtocolUtils.writeString(byteBuf, entity);
            }
        }
    }

    @NotNull
    private String getRGBChat(@NotNull String input) {
        if (!getPlugin().getFormatter().equals(Formatter.LEGACY)) {
            return getChatString(input);
        }

        final Component component = LegacyComponentSerializer.builder().hexColors().character('&').build().deserialize(input);

        return GsonComponentSerializer.gson().serialize(component);
    }
}
