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
import net.william278.velocitab.tab.Nametag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
@SuppressWarnings("unused")
public class UpdateTeamsPacket implements MinecraftPacket {

    private final Velocitab plugin;
    private String teamName;
    private UpdateMode mode;
    private Component displayName;
    private List<FriendlyFlag> friendlyFlags;
    private NametagVisibility nametagVisibility;
    private CollisionRule collisionRule;
    private int color;
    private Component prefix;
    private Component suffix;
    private List<String> entities;

    public UpdateTeamsPacket(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    public boolean isRemoveTeam() {
        return mode == UpdateMode.REMOVE_TEAM;
    }

    public boolean hasEntities() {
        return entities != null && !entities.isEmpty();
    }

    @NotNull
    protected static UpdateTeamsPacket create(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer,
                                              @NotNull String teamName, @NotNull Nametag nametag,
                                              @NotNull TabPlayer viewer,
                                              @NotNull String... teamMembers) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName)
                .mode(UpdateMode.CREATE_TEAM)
                .displayName(Component.empty())
                .friendlyFlags(List.of(FriendlyFlag.CAN_HURT_FRIENDLY))
                .nametagVisibility(isNametagPresent(nametag, plugin) ? NametagVisibility.ALWAYS : NametagVisibility.NEVER)
                .collisionRule(tabPlayer.getGroup().collisions() ? CollisionRule.ALWAYS : CollisionRule.NEVER)
                .color(getLastColor(tabPlayer, nametag.prefix(), plugin))
                .prefix(nametag.getPrefixComponent(plugin, tabPlayer, viewer))
                .suffix(nametag.getSuffixComponent(plugin, tabPlayer, viewer))
                .entities(Arrays.asList(teamMembers));
    }

    private static boolean isNametagPresent(@NotNull Nametag nametag, @NotNull Velocitab plugin) {
        if (!plugin.getSettings().isRemoveNametags()) {
            return true;
        }

        return !nametag.prefix().isEmpty() || !nametag.suffix().isEmpty();
    }

    @NotNull
    protected static UpdateTeamsPacket changeNametag(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer,
                                                     @NotNull String teamName, @NotNull TabPlayer viewer,
                                                     @NotNull Nametag nametag) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName)
                .mode(UpdateMode.UPDATE_INFO)
                .displayName(Component.empty())
                .friendlyFlags(List.of(FriendlyFlag.CAN_HURT_FRIENDLY))
                .nametagVisibility(isNametagPresent(nametag, plugin) ? NametagVisibility.ALWAYS : NametagVisibility.NEVER)
                .collisionRule(tabPlayer.getGroup().collisions() ? CollisionRule.ALWAYS : CollisionRule.NEVER)
                .color(getLastColor(tabPlayer, nametag.prefix(), plugin))
                .prefix(nametag.getPrefixComponent(plugin, tabPlayer, viewer))
                .suffix(nametag.getSuffixComponent(plugin, tabPlayer, viewer));
    }

    @NotNull
    protected static UpdateTeamsPacket addToTeam(@NotNull Velocitab plugin, @NotNull String teamName,
                                                 @NotNull String... teamMembers) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName)
                .mode(UpdateMode.ADD_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    protected static UpdateTeamsPacket removeFromTeam(@NotNull Velocitab plugin, @NotNull String teamName,
                                                      @NotNull String... teamMembers) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName)
                .mode(UpdateMode.REMOVE_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    protected static UpdateTeamsPacket removeTeam(@NotNull Velocitab plugin, @NotNull String teamName) {
        return new UpdateTeamsPacket(plugin)
                .teamName(teamName)
                .mode(UpdateMode.REMOVE_TEAM);
    }

    public static int getLastColor(@NotNull TabPlayer tabPlayer, @Nullable String text, @NotNull Velocitab plugin) {
        if (tabPlayer.getTeamColor() != null) {
            text = "&" + tabPlayer.getTeamColor().colorChar();
        }

        if (text == null) {
            return 15;
        }

        //add 1 random char at the end to make sure the last color is always found
        text = text + "z";

        //serialize & deserialize to downsample rgb to legacy
        final Component component = plugin.getFormatter().deserialize(text);
        text = LegacyComponentSerializer.legacyAmpersand().serialize(component);

        final int lastFormatIndex = text.lastIndexOf("&");
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
        BOLD('l', 17),
        STRIKETHROUGH('m', 18),
        UNDERLINED('n', 19),
        ITALIC('o', 20),
        RESET('r', 21);

        private static final Map<Character, TeamColor> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(TeamColor::colorChar, Function.identity()));

        @Getter
        private final char colorChar;
        private final int id;

        TeamColor(char colorChar, int id) {
            this.colorChar = colorChar;
            this.id = id;
        }

        public static int getColorId(char var) {
            return BY_ID.getOrDefault(var, TeamColor.RESET).id;
        }
    }

    @Override
    public void decode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        final ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        scoreboardManager.getPacketAdapter(protocolVersion).decode(byteBuf, this, protocolVersion);
    }

    @Override
    public void encode(ByteBuf byteBuf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        final ScoreboardManager scoreboardManager = plugin.getScoreboardManager();
        scoreboardManager.getPacketAdapter(protocolVersion).encode(byteBuf, this, protocolVersion);
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

        private static final Map<Byte, UpdateMode> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(UpdateMode::id, Function.identity()));

        private final byte id;

        UpdateMode(byte id) {
            this.id = id;
        }

        public byte id() {
            return id;
        }

        @Nullable
        public static UpdateMode byId(byte id) {
            return BY_ID.getOrDefault(id, null);
        }
    }

    public enum FriendlyFlag {
        CAN_HURT_FRIENDLY(0x01),
        CAN_HURT_FRIENDLY_FIRE(0x02);

        private static final List<FriendlyFlag> BY_ID = Arrays.stream(values()).toList();

        private final int id;

        FriendlyFlag(int id) {
            this.id = id;
        }

        @NotNull
        public static List<FriendlyFlag> fromBitMask(int bitMask) {
            return BY_ID.stream()
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

    public enum NametagVisibility {
        ALWAYS("always"),
        NEVER("never"),
        HIDE_FOR_OTHER_TEAMS("hideForOtherTeams"),
        HIDE_FOR_OWN_TEAM("hideForOwnTeam");

        private static final Map<String, NametagVisibility> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(NametagVisibility::id, Function.identity()));
        private static final Map<Integer, NametagVisibility> BY_ORDINAL = Arrays.stream(values())
                .collect(Collectors.toMap(NametagVisibility::ordinal, Function.identity()));

        private final String id;

        NametagVisibility(@NotNull String id) {
            this.id = id;
        }

        @NotNull
        public String id() {
            return id;
        }

        @NotNull
        public static NametagVisibility byId(@Nullable String id) {
            return id == null ? ALWAYS : BY_ID.getOrDefault(id, ALWAYS);
        }

        @NotNull
        public static NametagVisibility byOrdinal(int ordinal) {
            return BY_ORDINAL.getOrDefault(ordinal, ALWAYS);
        }
    }

    public enum CollisionRule {
        ALWAYS("always"),
        NEVER("never"),
        PUSH_OTHER_TEAMS("pushOtherTeams"),
        PUSH_OWN_TEAM("pushOwnTeam");

        private static final Map<String, CollisionRule> BY_ID = Arrays.stream(values())
                .collect(Collectors.toMap(CollisionRule::id, Function.identity()));
        private static final Map<Integer, CollisionRule> BY_ORDINAL = Arrays.stream(values())
                .collect(Collectors.toMap(CollisionRule::ordinal, Function.identity()));

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
            return id == null ? ALWAYS : BY_ID.getOrDefault(id, ALWAYS);
        }

        @NotNull
        public static CollisionRule byOrdinal(int ordinal) {
            return BY_ORDINAL.getOrDefault(ordinal, ALWAYS);
        }
    }
}
