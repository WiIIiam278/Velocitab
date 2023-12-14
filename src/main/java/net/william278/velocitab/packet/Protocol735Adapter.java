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
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Adapter for handling the UpdateTeamsPacket for Minecraft 1.16-1.20.2
 */
public class Protocol735Adapter extends Protocol404Adapter {

    private final GsonComponentSerializer serializer;

    public Protocol735Adapter(@NotNull Velocitab plugin) {
        super(plugin, Set.of(
                ProtocolVersion.MINECRAFT_1_16,
                ProtocolVersion.MINECRAFT_1_16_1,
                ProtocolVersion.MINECRAFT_1_16_2,
                ProtocolVersion.MINECRAFT_1_16_3,
                ProtocolVersion.MINECRAFT_1_16_4,
                ProtocolVersion.MINECRAFT_1_17,
                ProtocolVersion.MINECRAFT_1_17_1,
                ProtocolVersion.MINECRAFT_1_18,
                ProtocolVersion.MINECRAFT_1_18_2,
                ProtocolVersion.MINECRAFT_1_19,
                ProtocolVersion.MINECRAFT_1_19_1,
                ProtocolVersion.MINECRAFT_1_19_3,
                ProtocolVersion.MINECRAFT_1_19_4,
                ProtocolVersion.MINECRAFT_1_20,
                ProtocolVersion.MINECRAFT_1_20_2
        ));
        serializer = GsonComponentSerializer.gson();
    }

    @Override
    protected void writeComponent(ByteBuf buf, Component component) {
        ProtocolUtils.writeString(buf, serializer.serialize(component));
    }

}