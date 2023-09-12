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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("DuplicatedCode")
public class Protocol403Adapter extends TeamsPacketAdapter {

    public Protocol403Adapter() {
        super(Set.of(ProtocolVersion.MINECRAFT_1_13_2,
                ProtocolVersion.MINECRAFT_1_14_4,
                ProtocolVersion.MINECRAFT_1_15_2,
                ProtocolVersion.MINECRAFT_1_16_4,
                ProtocolVersion.MINECRAFT_1_17_1,
                ProtocolVersion.MINECRAFT_1_18_2,
                ProtocolVersion.MINECRAFT_1_19_4,
                ProtocolVersion.MINECRAFT_1_20
        ));
    }

    @Override
    public void decode(ByteBuf byteBuf, UpdateTeamsPacket updateTeamsPacket) {
        updateTeamsPacket.teamName(ProtocolUtils.readString(byteBuf));
        UpdateTeamsPacket.UpdateMode mode = UpdateTeamsPacket.UpdateMode.byId(byteBuf.readByte());
        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            updateTeamsPacket.displayName(ProtocolUtils.readString(byteBuf));
            updateTeamsPacket.friendlyFlags(UpdateTeamsPacket.FriendlyFlag.fromBitMask(byteBuf.readByte()));
            updateTeamsPacket.nameTagVisibility(UpdateTeamsPacket.NameTagVisibility.byId(ProtocolUtils.readString(byteBuf)));
            updateTeamsPacket.collisionRule(UpdateTeamsPacket.CollisionRule.byId(ProtocolUtils.readString(byteBuf)));
            updateTeamsPacket.color(byteBuf.readByte());
            updateTeamsPacket.prefix(ProtocolUtils.readString(byteBuf));
            updateTeamsPacket.suffix(ProtocolUtils.readString(byteBuf));
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            int entityCount = ProtocolUtils.readVarInt(byteBuf);
            List<String> entities = new ArrayList<>(entityCount);
            for (int j = 0; j < entityCount; j++) {
                entities.add(ProtocolUtils.readString(byteBuf));
            }
            updateTeamsPacket.entities(entities);
        }
    }

    @Override
    public void encode(ByteBuf byteBuf, UpdateTeamsPacket updateTeamsPacket) {
        ProtocolUtils.writeString(byteBuf, updateTeamsPacket.teamName());
        UpdateTeamsPacket.UpdateMode mode = updateTeamsPacket.mode();
        byteBuf.writeByte(mode.id());
        if (mode == UpdateTeamsPacket.UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.UPDATE_INFO) {
            ProtocolUtils.writeString(byteBuf, getChatString(updateTeamsPacket.displayName()));
            byteBuf.writeByte(UpdateTeamsPacket.FriendlyFlag.toBitMask(updateTeamsPacket.friendlyFlags()));
            ProtocolUtils.writeString(byteBuf, updateTeamsPacket.nameTagVisibility().id());
            ProtocolUtils.writeString(byteBuf, updateTeamsPacket.collisionRule().id());
            byteBuf.writeByte(updateTeamsPacket.color());
            ProtocolUtils.writeString(byteBuf, getChatString(updateTeamsPacket.prefix()));
            ProtocolUtils.writeString(byteBuf, getChatString(updateTeamsPacket.suffix()));
        }
        if (mode == UpdateTeamsPacket.UpdateMode.CREATE_TEAM || mode == UpdateTeamsPacket.UpdateMode.ADD_PLAYERS || mode == UpdateTeamsPacket.UpdateMode.REMOVE_PLAYERS) {
            List<String> entities = updateTeamsPacket.entities();
            ProtocolUtils.writeVarInt(byteBuf, entities != null ? entities.size() : 0);
            for (String entity : entities != null ? entities : new ArrayList<String>()) {
                ProtocolUtils.writeString(byteBuf, entity);
            }
        }
    }
}
