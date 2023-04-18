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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.velocitypowered.api.network.ProtocolVersion.*;
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
            final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
            connectedPlayer.getConnection().write(packet);
        } catch (Exception e) {
            plugin.log("Failed to dispatch packet (is the client or server modded or using an illegal version?)", e);
        }
    }

    private static final MethodHandle STATE_REGISTRY$clientBound;
    private static final MethodHandle PACKET_REGISTRY$register;
    private static final MethodHandle PACKET_MAPPING$map;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            final MethodHandles.Lookup stateRegistryLookup = MethodHandles.privateLookupIn(StateRegistry.class, lookup);
            STATE_REGISTRY$clientBound = stateRegistryLookup.findGetter(StateRegistry.class, "clientbound", StateRegistry.PacketRegistry.class);

            final MethodType mapType = MethodType.methodType(StateRegistry.PacketMapping.class, Integer.TYPE, ProtocolVersion.class, Boolean.TYPE);
            PACKET_MAPPING$map = stateRegistryLookup.findStatic(StateRegistry.class, "map", mapType);

            final MethodHandles.Lookup packetRegistryLookup = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, lookup);
            final MethodType registerType = MethodType.methodType(void.class, Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
            PACKET_REGISTRY$register = packetRegistryLookup.findVirtual(StateRegistry.PacketRegistry.class, "register", registerType);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void registerPacket() {
        StateRegistry registry = StateRegistry.PLAY;

        try {
            final StateRegistry.PacketMapping[] MAPPINGS = {
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x47, MINECRAFT_1_13, false),
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x4B, MINECRAFT_1_14, false),
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x4C, MINECRAFT_1_15, false),
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x55, MINECRAFT_1_17, false),
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x58, MINECRAFT_1_19_1, false),
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x56, MINECRAFT_1_19_3, false),
                    (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(0x5A, MINECRAFT_1_19_4, false)
            };
            final StateRegistry.PacketRegistry packetRegistry = (StateRegistry.PacketRegistry) STATE_REGISTRY$clientBound.invoke(registry);
            PACKET_REGISTRY$register.invoke(
                    packetRegistry,
                    UpdateTeamsPacket.class,
                    (Supplier<MinecraftPacket>) UpdateTeamsPacket::new,
                    MAPPINGS
            );
        } catch (Throwable e) {
            plugin.log("Failed to register UpdateTeamsPacket", e);
        }
    }


}
