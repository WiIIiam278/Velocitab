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
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_19, MINECRAFT_1_19, 0x55),
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
    public static UpdateTeamsPacket create(@NotNull String teamName, @NotNull String member) {
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        final UpdateTeamsPacket updateTeamsPacket = new UpdateTeamsPacket();
        updateTeamsPacket.teamName(teamName);
        updateTeamsPacket.mode(UpdateMode.CREATE);
        updateTeamsPacket.displayName(getChatString(teamName));
        updateTeamsPacket.friendlyFlags(List.of(FriendlyFlag.CAN_HURT_FRIENDLY));
        updateTeamsPacket.nameTagVisibility(NameTagVisibility.ALWAYS);
        updateTeamsPacket.collisionRule(CollisionRule.ALWAYS);
        updateTeamsPacket.color(15);
        updateTeamsPacket.prefix(getChatString(""));
        updateTeamsPacket.suffix(getChatString(""));
        updateTeamsPacket.entities(List.of(member));
        return updateTeamsPacket;
    }

    @NotNull
    public static UpdateTeamsPacket remove(@NotNull String teamName) {
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        final UpdateTeamsPacket updateTeamsPacket = new UpdateTeamsPacket();
        updateTeamsPacket.teamName(teamName);
        updateTeamsPacket.mode(UpdateMode.REMOVE);
        return updateTeamsPacket;
    }

    @Override
    public void read(ByteBuf byteBuf, PacketDirection packetDirection, int i) {
        teamName = ProtocolUtil.readString(byteBuf);
        mode = UpdateMode.byId(byteBuf.readByte());
        if (mode == UpdateMode.REMOVE) {
            return;
        }
        if (mode == UpdateMode.CREATE || mode == UpdateMode.UPDATE_INFO) {
            displayName = ProtocolUtil.readString(byteBuf);
            friendlyFlags = FriendlyFlag.fromBitMask(byteBuf.readByte());
            nameTagVisibility = NameTagVisibility.byId(ProtocolUtil.readString(byteBuf));
            collisionRule = CollisionRule.byId(ProtocolUtil.readString(byteBuf));
            color = byteBuf.readByte();
            prefix = ProtocolUtil.readString(byteBuf);
            suffix = ProtocolUtil.readString(byteBuf);
        }
        if (mode == UpdateMode.CREATE || mode == UpdateMode.ADD_PLAYERS || mode == UpdateMode.REMOVE_PLAYERS) {
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
        if (mode == UpdateMode.REMOVE) {
            return;
        }
        if (mode == UpdateMode.CREATE || mode == UpdateMode.UPDATE_INFO) {
            ProtocolUtil.writeString(byteBuf, displayName);
            byteBuf.writeByte(FriendlyFlag.toBitMask(friendlyFlags));
            ProtocolUtil.writeString(byteBuf, nameTagVisibility.id());
            ProtocolUtil.writeString(byteBuf, collisionRule.id());
            byteBuf.writeByte(color);
            ProtocolUtil.writeString(byteBuf, prefix);
            ProtocolUtil.writeString(byteBuf, suffix);
        }
        if (mode == UpdateMode.CREATE || mode == UpdateMode.ADD_PLAYERS || mode == UpdateMode.REMOVE_PLAYERS) {
            ProtocolUtil.writeVarInt(byteBuf, entities.size());
            for (String entity : entities) {
                ProtocolUtil.writeString(byteBuf, entity);
            }
        }
    }

    @NotNull
    private static String getChatString(@NotNull String string) {
        return "{\"text\":\"" + StringEscapeUtils.escapeJson(string) + "\"}";
    }

    public enum UpdateMode {
        CREATE(0),
        REMOVE(1),
        UPDATE_INFO(2),
        ADD_PLAYERS(3),
        REMOVE_PLAYERS(4);

        private final int id;

        UpdateMode(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static UpdateMode byId(int id) {
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

        NameTagVisibility(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static NameTagVisibility byId(String id) {
            return Arrays.stream(values())
                    .filter(visibility -> visibility.id.equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    public enum CollisionRule {
        ALWAYS("always"),
        NEVER("never"),
        PUSH_OTHER_TEAMS("pushOtherTeams"),
        PUSH_OWN_TEAM("pushOwnTeam");

        private final String id;

        CollisionRule(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static CollisionRule byId(String id) {
            return Arrays.stream(values())
                    .filter(rule -> rule.id.equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }
}
