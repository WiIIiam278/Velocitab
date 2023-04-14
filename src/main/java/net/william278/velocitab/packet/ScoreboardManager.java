package net.william278.velocitab.packet;

import com.velocitypowered.api.proxy.Player;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.Protocol;
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ScoreboardManager {

    private final Velocitab plugin;
    private final Map<UUID, List<String>> createdTeams;
    private final Map<UUID, Map<String, String>> roleMappings;

    public ScoreboardManager(@NotNull Velocitab velocitab) {
        this.plugin = velocitab;
        this.createdTeams = new HashMap<>();
        this.roleMappings = new HashMap<>();
    }

    public void resetCache(@NotNull Player player) {
        createdTeams.remove(player.getUniqueId());
        roleMappings.remove(player.getUniqueId());
    }

    public void setRoles(@NotNull Player player, @NotNull Map<String, String> playerRoles) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }
        playerRoles.entrySet().stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ))
                .forEach((role, players) -> updateRoles(player, role, players.toArray(new String[0])));
    }

    public void updateRoles(@NotNull Player player, @NotNull String role, @NotNull String... playerNames) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }
        if (!createdTeams.getOrDefault(player.getUniqueId(), List.of()).contains(role)) {
            dispatchPacket(UpdateTeamsPacket.create(role, playerNames), player);
            createdTeams.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(role);
            roleMappings.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(player.getUsername(), role);
        } else {
            roleMappings.getOrDefault(player.getUniqueId(), Map.of())
                    .entrySet().stream()
                    .filter((entry) -> List.of(playerNames).contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .forEach((playerName, oldRole) -> dispatchPacket(UpdateTeamsPacket.removeFromTeam(oldRole, playerName), player));
            dispatchPacket(UpdateTeamsPacket.addToTeam(role, playerNames), player);
            roleMappings.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(player.getUsername(), role);
        }
    }

    private void dispatchPacket(@NotNull UpdateTeamsPacket packet, @NotNull Player player) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }
        try {
            ProtocolizePlayer protocolizePlayer = Protocolize.playerProvider().player(player.getUniqueId());
            if (protocolizePlayer != null) {
                protocolizePlayer.sendPacket(packet);
            } else {
                plugin.log("Failed to get ProtocolizePlayer for player " + player.getUsername() + " (UUID: " + player.getUniqueId() + ")");
            }
        } catch (Exception e) {
            plugin.log("Failed to dispatch packet (is the client or server modded or using an illegal version?)", e);
        }
    }

    public void registerPacket() {
        try {
            Protocolize.protocolRegistration().registerPacket(
                    UpdateTeamsPacket.MAPPINGS,
                    Protocol.PLAY,
                    PacketDirection.CLIENTBOUND,
                    UpdateTeamsPacket.class
            );
        } catch (Exception e) {
            plugin.log("Failed to register UpdateTeamsPacket", e);
        }
    }


}
