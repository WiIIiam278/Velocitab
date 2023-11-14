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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class UpdateTeamsPacket implements MinecraftPacket {

    private final Velocitab plugin;

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

    public UpdateTeamsPacket(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    @NotNull
    protected static UpdateTeamsPacket create(@NotNull Velocitab plugin, @NotNull String teamName,
                                              @NotNull String displayName, @NotNull TabPlayer.Nametag nametag,
                                              @NotNull String... teamMembers) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.CREATE_TEAM)
                .displayName(displayName)
                .friendlyFlags(List.of(FriendlyFlag.CAN_HURT_FRIENDLY))
                .nameTagVisibility(isNametagPresent(nametag, plugin) ? NameTagVisibility.ALWAYS : NameTagVisibility.NEVER)
                .collisionRule(CollisionRule.ALWAYS)
                .color(getLastColor(nametag.prefix()))
                .prefix(nametag.prefix() == null ? "" : nametag.prefix())
                .suffix(nametag.suffix() == null ? "" : nametag.suffix())
                .entities(Arrays.asList(teamMembers));
    }

    private static boolean isNametagPresent(@NotNull TabPlayer.Nametag nametag, @NotNull Velocitab plugin) {
        if (!plugin.getSettings().isRemoveNametags()) {
            return true;
        }

        return nametag.prefix() != null && !nametag.prefix().isEmpty()
                || nametag.suffix() != null && !nametag.suffix().isEmpty();
    }

    @NotNull
    protected static UpdateTeamsPacket changeNameTag(@NotNull Velocitab plugin, @NotNull String teamName,
                                                     @NotNull TabPlayer.Nametag nametag) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.UPDATE_INFO)
                .displayName(teamName)
                .friendlyFlags(List.of(FriendlyFlag.CAN_HURT_FRIENDLY))
                .nameTagVisibility(isNametagPresent(nametag, plugin) ? NameTagVisibility.ALWAYS : NameTagVisibility.NEVER)
                .collisionRule(CollisionRule.ALWAYS)
                .color(getLastColor(nametag.prefix()))
                .prefix(nametag.prefix() == null ? "" : nametag.prefix())
                .suffix(nametag.suffix() == null ? "" : nametag.suffix());
    }

    @NotNull
    protected static UpdateTeamsPacket addToTeam(@NotNull Velocitab plugin, @NotNull String teamName,
                                                 @NotNull String... teamMembers) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.ADD_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    protected static UpdateTeamsPacket removeFromTeam(@NotNull Velocitab plugin, @NotNull String teamName,
                                                      @NotNull String... teamMembers) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.REMOVE_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    protected static UpdateTeamsPacket removeTeam(@NotNull Velocitab plugin, @NotNull String teamName) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.REMOVE_TEAM);
    }

    public static int getLastColor(@Nullable String text) {
        if (text == null) {
            return 15;
        }

        //add 1 random char at the end to make sure the last color is always found
        text = text + "z";

        //serialize & deserialize to downsample rgb to legacy
        Component legacyComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        text = LegacyComponentSerializer.legacyAmpersand().serialize(legacyComponent);

        int lastFormatIndex = text.lastIndexOf("&");
        if (lastFormatIndex == -1 || lastFormatIndex == text.length() - 1) {
            return 15;
        }

        final String last = text.substring(lastFormatIndex, lastFormatIndex + 2);
        return TeamColor.getColorId(last.charAt(1));
    }

    //Style-codes are handled as white
    public enum TeamColor {
        BLACK('0', 0),
        DARK_BLUE('1', 1),
        DARK_GREEN('2', 2),
        DARK_AQUA('3', 3),
        DARK_RED('4', 4),
        DARK_PURPLE('5', 5),
        GOLD('6', 6),
        GRAY('7', 7),
        DARK_GRAY('8', 8),
        BLUE('9', 9),
        GREEN('a', 10),
        AQUA('b', 11),
        RED('c', 12),
        LIGHT_PURPLE('d', 13),
        YELLOW('e', 14),
        WHITE('f', 15),
        OBFUSCATED('k', 16),
        BOLD('f', 17),
        STRIKETHROUGH('f', 18),
        UNDERLINED('f', 19),
        ITALIC('f', 20),
        RESET('r', 21);

        private final char colorChar;
        private final int id;

        TeamColor(char colorChar, int id) {
            this.colorChar = colorChar;
            this.id = id;
        }

        public static int getColorId(char var) {
            return Arrays.stream(values())
                    .filter(color -> color.colorChar == var)
                    .map(c -> c.id).findFirst()
                    .orElse(15);
        }
    }

    @Override
    public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        final Optional<ScoreboardManager> optionalManager = plugin.getScoreboardManager();

        if (optionalManager.isEmpty()) {
            return;
        }
        optionalManager.get().getPacketAdapter(protocolVersion).encode(byteBuf, this);
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

        @Nullable
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
