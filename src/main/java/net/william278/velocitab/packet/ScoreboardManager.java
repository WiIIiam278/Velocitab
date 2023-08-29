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
    private final Map<UUID, String> createdTeams;
    private final Map<String, String> nametags;

    public ScoreboardManager(@NotNull Velocitab velocitab) {
        this.plugin = velocitab;
        this.createdTeams = new ConcurrentHashMap<>();
        this.nametags = new ConcurrentHashMap<>();
    }

    public void resetCache(@NotNull Player player) {
        String team = createdTeams.remove(player.getUniqueId());
        if (team != null) {
            dispatchGroupPacket(UpdateTeamsPacket.removeTeam(team), player);
        }
    }

    public void updateRole(@NotNull Player player, @NotNull String role) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }

        String name = player.getUsername();

        TabPlayer tabPlayer = plugin.getTabPlayer(player);

        tabPlayer.getNametag(plugin).thenAccept(nametag -> {
            String[] split = nametag.split("%username%");
            String prefix = split[0];
            String suffix = split.length > 1 ? split[1] : "";

            if (!createdTeams.getOrDefault(player.getUniqueId(), "").equals(role)) {
                createdTeams.computeIfAbsent(player.getUniqueId(), k -> role);
                this.nametags.put(role, prefix + suffix);
                dispatchGroupPacket(UpdateTeamsPacket.create(role, "", prefix, suffix, new String[]{name}), player);
            } else if (!this.nametags.getOrDefault(role, "").equals(prefix + suffix)) {
                this.nametags.put(role, prefix + suffix);
                dispatchGroupPacket(UpdateTeamsPacket.changeNameTag(role, prefix, suffix), player);
            }
        }).exceptionally(e -> {
            plugin.log(Level.ERROR, "Failed to update role for " + player.getUsername(), e);
            return null;
        });
    }


    public void resendAllNameTags(Player player) {

        if(!plugin.getSettings().areNametagsEnabled()) {
            return;
        }

        Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();

        if (optionalServerConnection.isEmpty()) {
            return;
        }

        RegisteredServer serverInfo = optionalServerConnection.get().getServer();

        List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());

        List<Player> players = siblings.stream().map(RegisteredServer::getPlayersConnected).flatMap(Collection::stream).toList();

        players.forEach(p -> {
            if (p == player || !p.isActive()) {
                return;
            }

            String role = createdTeams.getOrDefault(p.getUniqueId(), "");

            if (role.isEmpty()) {
                return;
            }

            String nametag = nametags.getOrDefault(role, "");

            if (nametag.isEmpty()) {
                return;
            }

            String[] split = nametag.split("%username%");
            String prefix = split[0];
            String suffix = split.length > 1 ? split[1] : "";
            dispatchPacket(UpdateTeamsPacket.create(role, "", prefix, suffix, new String[]{p.getUsername()}), player);
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
        } catch (Exception e) {
            plugin.log(Level.ERROR, "Failed to dispatch packet (is the client or server modded or using an illegal version?)", e);
        }
    }

    private void dispatchGroupPacket(@NotNull UpdateTeamsPacket packet, @NotNull Player player) {
        Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();

        if (optionalServerConnection.isEmpty()) {
            return;
        }

        RegisteredServer serverInfo = optionalServerConnection.get().getServer();

        List<RegisteredServer> siblings = plugin.getTabList().getGroupServers(serverInfo.getServerInfo().getName());

        siblings.forEach(s -> {
            s.getPlayersConnected().forEach(p -> {
                try {
                    final ConnectedPlayer connectedPlayer = (ConnectedPlayer) p;
                    connectedPlayer.getConnection().write(packet);
                } catch (Exception e) {
                    plugin.log(Level.ERROR, "Failed to dispatch packet (is the client or server modded or using an illegal version?)", e);
                }
            });
        });
    }

    public void registerPacket() {
        try {
            packetRegistration = PacketRegistration.of(UpdateTeamsPacket.class)
                    .direction(ProtocolUtils.Direction.CLIENTBOUND)
                    .packetSupplier(UpdateTeamsPacket::new)
                    .stateRegistry(StateRegistry.PLAY)
                    .mapping(0x47, MINECRAFT_1_13, false)
                    .mapping(0x4B, MINECRAFT_1_14, false)
                    .mapping(0x4C, MINECRAFT_1_15, false)
                    .mapping(0x55, MINECRAFT_1_17, false)
                    .mapping(0x58, MINECRAFT_1_19_1, false)
                    .mapping(0x56, MINECRAFT_1_19_3, false)
                    .mapping(0x5A, MINECRAFT_1_19_4, false);
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
