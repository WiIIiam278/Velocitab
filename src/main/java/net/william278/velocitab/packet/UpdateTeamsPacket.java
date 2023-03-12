package net.william278.velocitab.packet;

import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
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

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class UpdateTeamsPacket extends AbstractPacket {

    protected static final List<ProtocolIdMapping> MAPPINGS = List.of(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x47),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_14_4, 0x4B),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_15, MINECRAFT_1_16_5, 0x4C),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_19, 0x55),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_1, MINECRAFT_1_19_2, 0x58),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19_3, MINECRAFT_LATEST, 0x56)
    );

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
    public static UpdateTeamsPacket create(@NotNull String teamName, @NotNull String... teamMembers) {
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
    public static UpdateTeamsPacket addToTeam(@NotNull String teamName, @NotNull String... teamMembers) {
        return new UpdateTeamsPacket()
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.ADD_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @NotNull
    public static UpdateTeamsPacket removeFromTeam(@NotNull String teamName, @NotNull String... teamMembers) {
        return new UpdateTeamsPacket()
                .teamName(teamName.length() > 16 ? teamName.substring(0, 16) : teamName)
                .mode(UpdateMode.REMOVE_PLAYERS)
                .entities(Arrays.asList(teamMembers));
    }

    @Override
    public void read(ByteBuf byteBuf, PacketDirection packetDirection, int i) {
        teamName = ProtocolUtil.readString(byteBuf);
        mode = UpdateMode.byId(byteBuf.readByte());
        if (mode == UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.UPDATE_INFO) {
            displayName = ProtocolUtil.readString(byteBuf);
            friendlyFlags = FriendlyFlag.fromBitMask(byteBuf.readByte());
            nameTagVisibility = NameTagVisibility.byId(ProtocolUtil.readString(byteBuf));
            collisionRule = CollisionRule.byId(ProtocolUtil.readString(byteBuf));
            color = byteBuf.readByte();
            prefix = ProtocolUtil.readString(byteBuf);
            suffix = ProtocolUtil.readString(byteBuf);
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.ADD_PLAYERS || mode == UpdateMode.REMOVE_PLAYERS) {
            int entityCount = ProtocolUtil.readVarInt(byteBuf);
            entities = new ArrayList<>(entityCount);
            for (int j = 0; j < entityCount; j++) {
                entities.add(ProtocolUtil.readString(byteBuf));
            }
        }
    }

    @Override
    public void write(ByteBuf byteBuf, PacketDirection packetDirection, int i) {
        ProtocolUtil.writeString(byteBuf, teamName);
        byteBuf.writeByte(mode.id());
        if (mode == UpdateMode.REMOVE_TEAM) {
            return;
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.UPDATE_INFO) {
            ProtocolUtil.writeString(byteBuf, displayName);
            byteBuf.writeByte(FriendlyFlag.toBitMask(friendlyFlags));
            ProtocolUtil.writeString(byteBuf, nameTagVisibility.id());
            ProtocolUtil.writeString(byteBuf, collisionRule.id());
            byteBuf.writeByte(color);
            ProtocolUtil.writeString(byteBuf, prefix);
            ProtocolUtil.writeString(byteBuf, suffix);
        }
        if (mode == UpdateMode.CREATE_TEAM || mode == UpdateMode.ADD_PLAYERS || mode == UpdateMode.REMOVE_PLAYERS) {
            ProtocolUtil.writeVarInt(byteBuf, entities != null ? entities.size() : 0);
            for (String entity : entities != null ? entities : new ArrayList<String>()) {
                ProtocolUtil.writeString(byteBuf, entity);
            }
        }
    }

    @NotNull
    private static String getChatString(@NotNull String string) {
        return "{\"text\":\"" + StringEscapeUtils.escapeJson(string) + "\"}";
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
