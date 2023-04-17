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
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import lombok.*;
import lombok.experimental.Accessors;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class UpdateTeamsPacket implements MinecraftPacket {

    private String teamName;
    private UpdateMode mode;
    private String displayName;
    private List<FriendlyFlag> friendlyFlags;
    private NameTagVisibility nameTagVisibility;
    private CollisionRule collisionRule;
    private int color;
    private String prefix;
    private String suffix;
    private List<String> entities;

    @NotNull
    protected static UpdateTeamsPacket create(@NotNull String teamName, @NotNull String... teamMembers) {
        return new UpdateTeamsPacket()
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.CREATE_TEAM)
                .displayName(getChatString(teamName))
                .friendlyFlags(List.of(FriendlyFlag.CAN_HURT_FRIENDLY))
                .nameTagVisibility(NameTagVisibility.ALWAYS)
                .collisionRule(CollisionRule.ALWAYS)
                .color(15)
                .prefix(getChatString(""))
                .suffix(getChatString(""))
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    protected static UpdateTeamsPacket addToTeam(@NotNull String teamName, @NotNull String... teamMembers) {
        return new UpdateTeamsPacket()
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.ADD_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    protected static UpdateTeamsPacket removeFromTeam(@NotNull String teamName, @NotNull String... teamMembers) {
        return new UpdateTeamsPacket()
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.REMOVE_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    private static String getChatString(@NotNull String string) {
        return "{\"text\":\"" + StringEscapeUtils.escapeJson(string) + "\"}";
    }

    @Override
    public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        teamName = ProtocolUtils.readString(byteBuf);
        mode = UpdateMode.byId(byteBuf.readByte());
        if (mode == UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.UPDATE_INFO) {
            displayName = ProtocolUtils.readString(byteBuf);
            friendlyFlags = FriendlyFlag.fromBitMask(byteBuf.readByte());
            nameTagVisibility = NameTagVisibility.byId(ProtocolUtils.readString(byteBuf));
            collisionRule = CollisionRule.byId(ProtocolUtils.readString(byteBuf));
            color = byteBuf.readByte();
            prefix = ProtocolUtils.readString(byteBuf);
            suffix = ProtocolUtils.readString(byteBuf);
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.ADD_PLAYERS || mode == UpdateMode.REMOVE_PLAYERS) {
            int entityCount = ProtocolUtils.readVarInt(byteBuf);
            entities = new ArrayList<>(entityCount);
            for (int j = 0; j < entityCount; j++) {
                entities.add(ProtocolUtils.readString(byteBuf));
            }
        }
    }

    @Override
    public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(byteBuf, teamName);
        byteBuf.writeByte(mode.id());
        if (mode == UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.UPDATE_INFO) {
            ProtocolUtils.writeString(byteBuf, displayName);
            byteBuf.writeByte(FriendlyFlag.toBitMask(friendlyFlags));
            ProtocolUtils.writeString(byteBuf, nameTagVisibility.id());
            ProtocolUtils.writeString(byteBuf, collisionRule.id());
            byteBuf.writeByte(color);
            ProtocolUtils.writeString(byteBuf, prefix);
            ProtocolUtils.writeString(byteBuf, suffix);
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.ADD_PLAYERS || mode == UpdateMode.REMOVE_PLAYERS) {
            ProtocolUtils.writeVarInt(byteBuf, entities != null ? entities.size() : 0);
            for (String entity : entities != null ? entities : new ArrayList<String>()) {
                ProtocolUtils.writeString(byteBuf, entity);
            }
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler minecraftSessionHandler) {
        return false;
    }

    public enum UpdateMode {
        CREATE_TEAM((byte) 0),
        REMOVE_TEAM((byte) 1),
        UPDATE_INFO((byte) 2),
        ADD_PLAYERS((byte) 3),
        REMOVE_PLAYERS((byte) 4);

        private final byte id;

        UpdateMode(byte id) {
            this.id = id;
        }

        public byte id() {
            return id;
        }

        public static UpdateMode byId(byte id) {
            return Arrays.stream(values())
                    .filter(mode -> mode.id == id)
                    .findFirst()
                    .orElse(null);
        }
    }

    public enum FriendlyFlag {
        CAN_HURT_FRIENDLY(0x01),
        CAN_HURT_FRIENDLY_FIRE(0x02);

        private final int id;

        FriendlyFlag(int id) {
            this.id = id;
        }

        @NotNull
        public static List<FriendlyFlag> fromBitMask(int bitMask) {
            return Arrays.stream(values())
                    .filter(flag -> (bitMask & flag.id) != 0)
                    .collect(Collectors.toList());
        }

        public static int toBitMask(@NotNull List<FriendlyFlag> friendlyFlags) {
            int bitMask = 0;
            for (FriendlyFlag friendlyFlag : friendlyFlags) {
                bitMask |= friendlyFlag.id;
            }
            return bitMask;
        }
    }

    public enum NameTagVisibility {
        ALWAYS("always"),
        NEVER("never"),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam");

        private final String id;

        NameTagVisibility(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public String id() {
            return id;
        }

        @NotNull
        public static NameTagVisibility byId(@Nullable String id) {
            return id == null ? ALWAYS : Arrays.stream(values())
                    .filter(visibility -> visibility.id.equals(id))
                    .findFirst()
                    .orElse(ALWAYS);
        }
    }

    public enum CollisionRule {
        ALWAYS("always"),
        NEVER("never"),
        PUSH_OTHER_TEAMS("pushOtherTeams"),
        PUSH_OWN_TEAM("pushOwnTeam");

        private final String id;

        CollisionRule(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public String id() {
            return id;
        }

        @NotNull
        public static CollisionRule byId(@Nullable String id) {
            return id == null ? ALWAYS : Arrays.stream(values())
                    .filter(rule -> rule.id.equals(id))
                    .findFirst()
                    .orElse(ALWAYS);
        }
    }
}
