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
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.william278.velocitab.Velocitab;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public abstract class TeamsPacketAdapter {

    private final Velocitab plugin;
    private final Set<ProtocolVersion> protocolVersions;

    public abstract void encode(@NotNull ByteBuf byteBuf, @NotNull UpdateTeamsPacket packet);

    @NotNull
    protected String getChatString(@NotNull String string) {
        return String.format("{\"text\":\"%s\"}", StringEscapeUtils.escapeJson(string));
    }


}
