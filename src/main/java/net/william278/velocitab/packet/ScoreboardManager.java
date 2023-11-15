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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.velocitypowered.api.network.ProtocolVersion.*;

public class ScoreboardManager {

    private PacketRegistration<UpdateTeamsPacket> packetRegistration;
    private final Velocitab plugin;
    private final Set<TeamsPacketAdapter> versions;
    private final Map<UUID, String> createdTeams;
    private final Map<String, TabPlayer.Nametag> nameTags;

    public ScoreboardManager(@NotNull Velocitab velocitab) {
        this.plugin = velocitab;
        this.createdTeams = new ConcurrentHashMap<>();
        this.nameTags = new ConcurrentHashMap<>();
        this.versions = new HashSet<>();
        this.registerVersions();
    }

    private void registerVersions() {
        versions.add(new Protocol403Adapter(plugin));
        versions.add(new Protocol340Adapter(plugin));
        versions.add(new Protocol48Adapter(plugin));
    }

    @NotNull
    public TeamsPacketAdapter getPacketAdapter(@NotNull ProtocolVersion version) {
        return versions.stream()
                .filter(adapter -> adapter.getProtocolVersions().contains(version))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No adapter found for protocol version " + version));
    }

    public void close() {
        plugin.getServer().getAllPlayers().forEach(this::resetCache);
    }

    public void resetCache(@NotNull Player player) {
        final String team = createdTeams.remove(player.getUniqueId());
        if (team != null) {
            dispatchGroupPacket(UpdateTeamsPacket.removeTeam(plugin, team), player);
        }
    }

    public void vanishPlayer(@NotNull Player player) {
        if (!plugin.getSettings().isSortPlayers()) {
            return;
        }

        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final RegisteredServer serverInfo = optionalServerConnection.get().getServer();
        final List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());

        final String teamName = createdTeams.get(player.getUniqueId());
        if (teamName == null) {
            return;
        }

        final UpdateTeamsPacket packet = UpdateTeamsPacket.removeTeam(plugin, teamName);
        siblings.forEach(server -> server.getPlayersConnected().forEach(connected -> {
            final boolean canSee = plugin.getVanishManager().canSee(connected.getUsername(), player.getUsername());

            if (canSee) {
                return;
            }

            dispatchPacket(packet, connected);
        }));
    }

    public void unVanishPlayer(@NotNull Player player) {
        if (!plugin.getSettings().isSortPlayers()) {
            return;
        }

        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final RegisteredServer serverInfo = optionalServerConnection.get().getServer();
        final List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());
        final String teamName = createdTeams.get(player.getUniqueId());
        if (teamName == null) {
            return;
        }

        final Optional<TabPlayer.Nametag> cachedTag = Optional.ofNullable(nameTags.getOrDefault(teamName, null));
        cachedTag.ifPresent(nametag -> {
            final UpdateTeamsPacket packet = UpdateTeamsPacket.create(
                    plugin, createdTeams.get(player.getUniqueId()),
                    "", nametag, player.getUsername()
            );
            siblings.forEach(server -> server.getPlayersConnected().stream().filter(p -> p != player)
                    .forEach(connected -> dispatchPacket(packet, connected)));
        });
    }

    public void updateRole(@NotNull Player player, @NotNull String role) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }

        final String name = player.getUsername();
        final TabPlayer tabPlayer = plugin.getTabList().getTabPlayer(player).orElseThrow();
        tabPlayer.getNametag(plugin).thenAccept(tag -> {
            final TabPlayer.Nametag newTag = new TabPlayer.Nametag(tag, player);
            if (!createdTeams.getOrDefault(player.getUniqueId(), "").equals(role)) {
                if (createdTeams.containsKey(player.getUniqueId())) {
                    dispatchGroupPacket(
                            UpdateTeamsPacket.removeTeam(plugin, createdTeams.get(player.getUniqueId())),
                            player
                    );
                }

                createdTeams.put(player.getUniqueId(), role);
                this.nameTags.put(role, newTag);
                dispatchGroupPacket(
                        UpdateTeamsPacket.create(plugin, role, "", newTag, name),
                        player
                );
            } else if (!this.nameTags.containsKey(role) && this.nameTags.get(role).equals(newTag)) {
                this.nameTags.put(role, newTag);
                dispatchGroupPacket(
                        UpdateTeamsPacket.changeNameTag(plugin, role, newTag),
                        player
                );
            }
        }).exceptionally(e -> {
            plugin.log(Level.ERROR, "Failed to update role for " + player.getUsername(), e);
            return null;
        });
    }


    public void resendAllTeams(@NotNull Player player) {
        if (!plugin.getSettings().isSendScoreboardPackets()) {
            return;
        }

        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final RegisteredServer serverInfo = optionalServerConnection.get().getServer();
        final List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());
        final List<Player> players = siblings.stream()
                .map(RegisteredServer::getPlayersConnected)
                .flatMap(Collection::stream)
                .toList();

        final List<String> roles = new ArrayList<>();
        players.forEach(p -> {
            if (p == player || !p.isActive()) {
                return;
            }

            if (!plugin.getVanishManager().canSee(player.getUsername(), p.getUsername())) {
                return;
            }

            final String role = createdTeams.getOrDefault(p.getUniqueId(), "");
            if (role.isEmpty()) {
                return;
            }

            // Prevent duplicate packets
            if (roles.contains(role)) {
                return;
            }
            roles.add(role);

            // Send packet
            final TabPlayer.Nametag tag = nameTags.get(role);
            if (tag != null) {
                final TabPlayer.Nametag nametag = nameTags.get(role);
                final UpdateTeamsPacket packet = UpdateTeamsPacket.create(
                        plugin, role, "", nametag, p.getUsername()
                );
                dispatchPacket(packet, player);
            }
        });
    }

    private void dispatchPacket(@NotNull UpdateTeamsPacket packet, @NotNull Player player) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }

        try {
            final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
            connectedPlayer.getConnection().write(packet);
        } catch (Throwable e) {
            plugin.log(Level.ERROR, "Failed to dispatch packet (unsupported client or server version)", e);
        }
    }

    private void dispatchGroupPacket(@NotNull UpdateTeamsPacket packet, @NotNull Player player) {
        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final RegisteredServer serverInfo = optionalServerConnection.get().getServer();
        final List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());
        siblings.forEach(server -> server.getPlayersConnected().forEach(connected -> {
            try {
                final boolean canSee = plugin.getVanishManager().canSee(connected.getUsername(), player.getUsername());
                if (!canSee) {
                    return;
                }

                final ConnectedPlayer connectedPlayer = (ConnectedPlayer) connected;
                connectedPlayer.getConnection().write(packet);
            } catch (Throwable e) {
                plugin.log(Level.ERROR, "Failed to dispatch packet (unsupported client or server version)", e);
            }
        }));
    }

    public void registerPacket() {
        try {
            packetRegistration = PacketRegistration.of(UpdateTeamsPacket.class)
                    .direction(ProtocolUtils.Direction.CLIENTBOUND)
                    .packetSupplier(() -> new UpdateTeamsPacket(plugin))
                    .stateRegistry(StateRegistry.PLAY)
                    .mapping(0x3E, MINECRAFT_1_8, true)
                    .mapping(0x44, MINECRAFT_1_12_2, true)
                    .mapping(0x47, MINECRAFT_1_13, true)
                    .mapping(0x4B, MINECRAFT_1_14, true)
                    .mapping(0x4C, MINECRAFT_1_15, true)
                    .mapping(0x55, MINECRAFT_1_17, true)
                    .mapping(0x58, MINECRAFT_1_19_1, true)
                    .mapping(0x56, MINECRAFT_1_19_3, true)
                    .mapping(0x5A, MINECRAFT_1_19_4, true)
                    .mapping(0x5C, MINECRAFT_1_20_2, true);
            packetRegistration.register();
        } catch (Throwable e) {
            plugin.log(Level.ERROR, "Failed to register UpdateTeamsPacket", e);
        }
    }

    public void unregisterPacket() {
        if (packetRegistration == null) {
            return;
        }
        try {
            packetRegistration.unregister();
        } catch (Throwable e) {
            plugin.log(Level.ERROR, "Failed to unregister UpdateTeamsPacket", e);
        }
    }

    /**
     * Recalculates the vanish status for a specific player.
     * This method updates the player's scoreboard to reflect the vanish status of another player.
     *
     * @param tabPlayer The TabPlayer object representing the player whose scoreboard will be updated.
     * @param target    The TabPlayer object representing the player whose vanish status will be reflected.
     * @param canSee    A boolean indicating whether the player can see the target player.
     */
    public void recalculateVanishForPlayer(TabPlayer tabPlayer, TabPlayer target, boolean canSee) {
        final Player player = tabPlayer.getPlayer();
        final String team = createdTeams.get(target.getPlayer().getUniqueId());
        if (team == null) {
            return;
        }

        final UpdateTeamsPacket removeTeam = UpdateTeamsPacket.removeTeam(plugin, team);
        dispatchPacket(removeTeam, player);

        if (canSee) {
            final TabPlayer.Nametag tag = nameTags.get(team);
            if (tag != null) {
                final UpdateTeamsPacket addTeam = UpdateTeamsPacket.create(
                        plugin, team, "", tag, target.getPlayer().getUsername()
                );
                dispatchPacket(addTeam, player);
            }
        }
    }

}
