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
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.*;
import java.util.stream.Collectors;

import static com.velocitypowered.api.network.ProtocolVersion.*;

public class ScoreboardManager {

    private PacketRegistration<UpdateTeamsPacket> packetRegistration;
    private final Velocitab plugin;
    private final Map<UUID, List<String>> createdTeams;
    private final Map<UUID, Map<String, String>> roleMappings;
    private final Set<TeamsPacketAdapter> versions;

    public ScoreboardManager(@NotNull Velocitab velocitab) {
        this.plugin = velocitab;
        this.createdTeams = new HashMap<>();
        this.roleMappings = new HashMap<>();
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
        System.out.println("updateRoles " + playerNames.length + " " + role + " " + player.getUsername());
        if (!createdTeams.getOrDefault(player.getUniqueId(), List.of()).contains(role)) {
            dispatchPacket(UpdateTeamsPacket.create(plugin, role, playerNames), player);
            createdTeams.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(role);
            roleMappings.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(player.getUsername(), role);
        } else {
            roleMappings.getOrDefault(player.getUniqueId(), Map.of())
                    .entrySet().stream()
                    .filter((entry) -> List.of(playerNames).contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    .forEach((playerName, oldRole) -> dispatchPacket(
                            UpdateTeamsPacket.removeFromTeam(plugin, oldRole, playerName),
                            player
                    ));
            dispatchPacket(UpdateTeamsPacket.addToTeam(plugin, role, playerNames), player);
            roleMappings.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(player.getUsername(), role);
        }
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

    public void registerPacket() {
        try {
            packetRegistration = PacketRegistration.of(UpdateTeamsPacket.class)
                    .direction(ProtocolUtils.Direction.CLIENTBOUND)
                    .packetSupplier(() -> new UpdateTeamsPacket(plugin))
                    .stateRegistry(StateRegistry.PLAY)
                    .mapping(0x3E, MINECRAFT_1_8, false)
                    .mapping(0x44, MINECRAFT_1_12_2, false)
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
