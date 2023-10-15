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

    private static final String NAMETAG_DELIMITER = ":::";
    private PacketRegistration<UpdateTeamsPacket> packetRegistration;
    private final Velocitab plugin;
    private final Set<TeamsPacketAdapter> versions;
    private final Map<UUID, String> createdTeams;
    private final Map<String, String> nametags;

    public ScoreboardManager(@NotNull Velocitab velocitab) {
        this.plugin = velocitab;
        this.createdTeams = new ConcurrentHashMap<>();
        this.nametags = new ConcurrentHashMap<>();
        this.versions = new HashSet<>();
        this.registerVersions();
    }

    private void registerVersions() {
        versions.add(new Protocol403Adapter());
        versions.add(new Protocol340Adapter());
        versions.add(new Protocol48Adapter());
    }

    @NotNull
    public TeamsPacketAdapter getPacketAdapter(@NotNull ProtocolVersion version) {
        return versions.stream()
                .filter(adapter -> adapter.getProtocolVersions().contains(version))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No adapter found for protocol version " + version));
    }

    public void resetCache(@NotNull Player player) {
        final String team = createdTeams.remove(player.getUniqueId());
        if (team != null) {
            dispatchGroupPacket(UpdateTeamsPacket.removeTeam(plugin, team), player);
        }
    }

    public void vanishPlayer(Player player) {
        if (!plugin.getSettings().isSortPlayers()) {
            return;
        }

        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final RegisteredServer serverInfo = optionalServerConnection.get().getServer();
        final List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());
        UpdateTeamsPacket packet = UpdateTeamsPacket.removeTeam(plugin, createdTeams.get(player.getUniqueId()));

        siblings.forEach(server -> server.getPlayersConnected().forEach(connected -> {
            boolean canSee = !plugin.getVanishManager().isVanished(connected.getUsername())
                    || plugin.getVanishManager().canSee(player.getUsername(), player.getUsername());

            if (!canSee) {
                return;
            }

            dispatchPacket(packet, connected);
        }));
    }

    public void unvanishPlayer(Player player) {
        if (!plugin.getSettings().isSortPlayers()) {
            return;
        }

        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final RegisteredServer serverInfo = optionalServerConnection.get().getServer();
        final List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());

        final String role = createdTeams.getOrDefault(player.getUniqueId(), "");
        if (role.isEmpty()) {
            return;
        }

        final String nametag = nametags.getOrDefault(role, "");
        if (nametag.isEmpty()) {
            return;
        }

        final String[] split = nametag.split(NAMETAG_DELIMITER, 2);
        final String prefix = split[0];
        final String suffix = split.length > 1 ? split[1] : "";

        final UpdateTeamsPacket packet = UpdateTeamsPacket.create(plugin, createdTeams.get(player.getUniqueId()), "", prefix, suffix, player.getUsername());

        siblings.forEach(server -> server.getPlayersConnected().forEach(connected -> dispatchPacket(packet, connected)));
    }

    public void updateRole(@NotNull Player player, @NotNull String role) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }

        final String name = player.getUsername();
        final TabPlayer tabPlayer = plugin.getTabList().getTabPlayer(player).orElseThrow();
        tabPlayer.getNametag(plugin).thenAccept(nametag -> {
            String[] split = nametag.split(player.getUsername(), 2);
            String prefix = split[0];
            String suffix = split.length > 1 ? split[1] : "";

            if (!createdTeams.getOrDefault(player.getUniqueId(), "").equals(role)) {
                if (createdTeams.containsKey(player.getUniqueId())) {
                    dispatchGroupPacket(UpdateTeamsPacket.removeTeam(plugin, createdTeams.get(player.getUniqueId())), player);
                }

                createdTeams.put(player.getUniqueId(), role);
                this.nametags.put(role, prefix + NAMETAG_DELIMITER + suffix);
                dispatchGroupPacket(UpdateTeamsPacket.create(plugin, role, "", prefix, suffix, name), player);
            } else if (!this.nametags.getOrDefault(role, "").equals(prefix + NAMETAG_DELIMITER + suffix)) {
                this.nametags.put(role, prefix + NAMETAG_DELIMITER + suffix);
                dispatchGroupPacket(UpdateTeamsPacket.changeNameTag(plugin, role, prefix, suffix), player);
            }
        }).exceptionally(e -> {
            plugin.log(Level.ERROR, "Failed to update role for " + player.getUsername(), e);
            return null;
        });
    }


    public void resendAllNameTags(Player player) {
        if (!plugin.getSettings().doNametags()) {
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
        players.forEach(p -> {
            if (p == player || !p.isActive()) {
                return;
            }

            final String role = createdTeams.getOrDefault(p.getUniqueId(), "");
            if (role.isEmpty()) {
                return;
            }

            final String nametag = nametags.getOrDefault(role, "");
            if (nametag.isEmpty()) {
                return;
            }

            final String[] split = nametag.split(NAMETAG_DELIMITER, 2);
            final String prefix = split[0];
            final String suffix = split.length > 1 ? split[1] : "";
            dispatchPacket(UpdateTeamsPacket.create(plugin, role, "", prefix, suffix, p.getUsername()), player);
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

                boolean canSee = !plugin.getVanishManager().isVanished(connected.getUsername())
                        || plugin.getVanishManager().canSee(player.getUsername(), player.getUsername());

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


}
