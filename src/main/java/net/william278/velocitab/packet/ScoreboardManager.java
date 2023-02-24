package net.william278.velocitab.packet;

import com.velocitypowered.api.proxy.Player;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final Velocitab plugin;

    private final ConcurrentHashMap<UUID, String> fauxTeams;

    public ScoreboardManager(@NotNull Velocitab velocitab) {
        this.plugin = velocitab;
        this.fauxTeams = new ConcurrentHashMap<>();
    }

    public void registerPacket() {
        Protocolize.protocolRegistration().registerPacket(
                UpdateTeamsPacket.MAPPINGS,
                Protocol.PLAY,
                PacketDirection.CLIENTBOUND,
                UpdateTeamsPacket.class
        );
    }

    public void setPlayerTeam(@NotNull TabPlayer player) {
        removeTeam(player.getPlayer());
        createTeam(player.getTeamName(), player.getPlayer());
    }

    private void createTeam(@NotNull String teamName, @NotNull Player member) {
        final UUID uuid = member.getUniqueId();
        final UpdateTeamsPacket createTeamPacket = UpdateTeamsPacket.create(teamName, member.getUsername());
        plugin.getServer().getAllPlayers().stream()
                .map(Player::getUniqueId)
                .map(Protocolize.playerProvider()::player)
                .forEach(protocolPlayer -> protocolPlayer.sendPacket(createTeamPacket));
        fauxTeams.put(uuid, teamName);
    }

    public void removeTeam(@NotNull Player member) {
        final UUID uuid = member.getUniqueId();
        final String teamName = fauxTeams.getOrDefault(uuid, null);
        if (teamName != null) {
            final UpdateTeamsPacket removeTeamPacket = UpdateTeamsPacket.remove(teamName);
            plugin.getServer().getAllPlayers().stream()
                    .map(Player::getUniqueId)
                    .map(Protocolize.playerProvider()::player)
                    .forEach(protocolPlayer -> protocolPlayer.sendPacket(removeTeamPacket));
        }
        fauxTeams.remove(uuid);
    }

}
