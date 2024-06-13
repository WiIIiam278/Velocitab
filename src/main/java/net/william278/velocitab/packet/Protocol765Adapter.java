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

import java.util.Set;

/**
 * Adapter for handling the UpdateTeamsPacket for Minecraft 1.20.3-1.20.5
 */
public class Protocol765Adapter extends Protocol404Adapter {

    public Protocol765Adapter(@NotNull Velocitab plugin) {
        super(plugin, Set.of(
                ProtocolVersion.MINECRAFT_1_20_3,
                ProtocolVersion.MINECRAFT_1_20_5,
                ProtocolVersion.MINECRAFT_1_21
        ));
    }

    protected void writeComponent(ByteBuf buf, Component component) {
        final BinaryTag tag = ComponentHolder.serialize(GsonComponentSerializer.gson().serializeToTree(component));
        ProtocolUtils.writeBinaryTag(buf, ProtocolVersion.MINECRAFT_1_20_3, tag);
    }

}
