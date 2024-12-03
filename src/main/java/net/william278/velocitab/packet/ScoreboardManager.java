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

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.sorting.SortedSet;
import net.william278.velocitab.tab.Nametag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.velocitypowered.api.network.ProtocolVersion.*;

public class ScoreboardManager {

    private PacketRegistration<UpdateTeamsPacket> packetRegistration;
    private final Velocitab plugin;
    private final boolean teams;
    private final Map<ProtocolVersion, TeamsPacketAdapter> versions;
    @Getter
    private final Map<UUID, String> createdTeams;
    private final Map<String, Nametag> nametags;
    private final Multimap<UUID, String> trackedTeams;
    @Getter
    private final SortedSet sortedTeams;

    public ScoreboardManager(@NotNull Velocitab velocitab, boolean teams) {
        this.plugin = velocitab;
        this.teams = teams;
        this.createdTeams = Maps.newConcurrentMap();
        this.nametags = Maps.newConcurrentMap();
        this.versions = Maps.newHashMap();
        this.trackedTeams = Multimaps.synchronizedMultimap(Multimaps.newSetMultimap(Maps.newConcurrentMap(), Sets::newConcurrentHashSet));
        this.sortedTeams = new SortedSet(Comparator.reverseOrder());
        this.registerVersions();
    }

    public boolean handleTeams() {
        return teams;
    }

    private void registerVersions() {
        try {
            final Protocol765Adapter protocol765Adapter = new Protocol765Adapter(plugin);
            protocol765Adapter.getProtocolVersions().forEach(version -> versions.put(version, protocol765Adapter));
            final Protocol735Adapter protocol735Adapter = new Protocol735Adapter(plugin);
            protocol735Adapter.getProtocolVersions().forEach(version -> versions.put(version, protocol735Adapter));
            final Protocol404Adapter protocol404Adapter = new Protocol404Adapter(plugin);
            protocol404Adapter.getProtocolVersions().forEach(version -> versions.put(version, protocol404Adapter));
            final Protocol48Adapter protocol48Adapter = new Protocol48Adapter(plugin);
            protocol48Adapter.getProtocolVersions().forEach(version -> versions.put(version, protocol48Adapter));
        } catch (NoSuchFieldError e) {
            throw new IllegalStateException("Failed to register Scoreboard Teams packets." +
                                            " Velocitab probably does not (yet) support your Proxy version.", e);
        }
    }

    public boolean isInternalTeam(@NotNull String teamName) {
        return nametags.containsKey(teamName);
    }

    public int getPosition(@NotNull String teamName) {
        return sortedTeams.getPosition(teamName);
    }

    @NotNull
    public TeamsPacketAdapter getPacketAdapter(@NotNull ProtocolVersion version) {
        return Optional.ofNullable(versions.get(version))
                .orElseThrow(() -> new IllegalArgumentException("No adapter found for protocol version " + version));
    }

    public void close() {
        plugin.getServer().getAllPlayers().forEach(this::resetCache);
    }

    public void resetCache(@NotNull Player player) {
        final String team = createdTeams.remove(player.getUniqueId());
        if (team != null) {
            removeSortedTeam(team);
            plugin.getTabList().getTabPlayer(player).ifPresent(tabPlayer ->
                    dispatchGroupPacket(UpdateTeamsPacket.removeTeam(plugin, team), tabPlayer)
            );
            trackedTeams.removeAll(player.getUniqueId());
        }
    }

    public void resetCache(@NotNull Player player, @NotNull Group group) {
        final String team = createdTeams.remove(player.getUniqueId());
        if (team != null) {
            removeSortedTeam(team);
            dispatchGroupPacket(UpdateTeamsPacket.removeTeam(plugin, team), group);
        }
    }

    private void removeSortedTeam(@NotNull String teamName) {
        final boolean result = sortedTeams.removeTeam(teamName);
        if (!result) {
            plugin.log(Level.ERROR, "Failed to remove team " + teamName + " from sortedTeams");
        }
    }

    public void vanishPlayer(@NotNull TabPlayer tabPlayer) {
        this.handleVanish(tabPlayer, true);
    }

    public void unVanishPlayer(@NotNull TabPlayer tabPlayer) {
        this.handleVanish(tabPlayer, false);
    }

    private void handleVanish(@NotNull TabPlayer tabPlayer, boolean vanish) {
        if (!plugin.getSettings().isSortPlayers()) {
            return;
        }

        final Player player = tabPlayer.getPlayer();
        final String teamName = createdTeams.get(player.getUniqueId());
        if (teamName == null) {
            return;
        }
        final Set<RegisteredServer> siblings = tabPlayer.getGroup().registeredServers(plugin);
        final boolean isNameTagEmpty = tabPlayer.getGroup().nametag().isEmpty() && !plugin.getSettings().isRemoveNametags();

        final Optional<Nametag> cachedTag = Optional.ofNullable(nametags.getOrDefault(teamName, null));
        cachedTag.ifPresent(nametag -> siblings.forEach(server -> server.getPlayersConnected().stream().filter(p -> p != player)
                .forEach(connected -> {
                    if (vanish && !plugin.getVanishManager().canSee(connected.getUsername(), player.getUsername())) {
                        sendPacket(connected, UpdateTeamsPacket.removeTeam(plugin, teamName), isNameTagEmpty);
                        trackedTeams.remove(connected.getUniqueId(), teamName);
                    } else {
                        dispatchGroupCreatePacket(plugin, tabPlayer, teamName, nametag, player.getUsername());
                    }
                })));
    }

    /**
     * Updates the role of the player in the scoreboard.
     *
     * @param tabPlayer The TabPlayer object representing the player whose role will be updated.
     * @param role      The new role of the player. Must not be null.
     * @param force     Whether to force the update even if the player's nametag is the same.
     */
    public CompletableFuture<Void> updateRole(@NotNull TabPlayer tabPlayer, @NotNull String role, boolean force) {
        final Player player = tabPlayer.getPlayer();
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return CompletableFuture.completedFuture(null);
        }
        final String name = player.getUsername();
        final CompletableFuture<Void> future = new CompletableFuture<>();
        tabPlayer.getNametag(plugin).thenAccept(newTag -> {
            if (!createdTeams.getOrDefault(player.getUniqueId(), "").equals(role)) {
                if (createdTeams.containsKey(player.getUniqueId())) {
                    dispatchGroupPacket(
                            UpdateTeamsPacket.removeTeam(plugin, createdTeams.get(player.getUniqueId())),
                            tabPlayer
                    );
                }
                final String oldRole = createdTeams.remove(player.getUniqueId());
                if (oldRole != null) {
                    removeSortedTeam(oldRole);
                }
                createdTeams.put(player.getUniqueId(), role);
                final boolean a = sortedTeams.addTeam(role);
                if (!a) {
                    plugin.log(Level.ERROR, "Failed to add team " + role + " to sortedTeams");
                }
                this.nametags.put(role, newTag);
                dispatchGroupCreatePacket(plugin, tabPlayer, role, newTag, name);
            } else if (force || (this.nametags.containsKey(role) && !this.nametags.get(role).equals(newTag))) {
                this.nametags.put(role, newTag);
                dispatchGroupChangePacket(plugin, tabPlayer, role, newTag);
            } else {
                updatePlaceholders(tabPlayer);
            }
            future.complete(null);
        }).exceptionally(e -> {
            plugin.log(Level.ERROR, "Failed to update role for " + player.getUsername(), e);
            return null;
        });

        return future;
    }

    public void updatePlaceholders(@NotNull TabPlayer tabPlayer) {
        final Player player = tabPlayer.getPlayer();

        final String role = createdTeams.get(player.getUniqueId());
        if (role == null) {
            return;
        }

        final Optional<Nametag> optionalNametag = Optional.ofNullable(nametags.get(role));
        optionalNametag.ifPresent(nametag -> dispatchGroupChangePacket(plugin, tabPlayer, role, nametag));
    }

    public void resendAllTeams(@NotNull TabPlayer tabPlayer) {
        if (!teams) {
            return;
        }
        if (!plugin.getSettings().isSendScoreboardPackets()) {
            return;
        }

        final Player player = tabPlayer.getPlayer();
        final Set<Player> players = tabPlayer.getGroup().getPlayers(plugin, tabPlayer);

        final Set<String> roles = Sets.newHashSet();
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

            final Optional<TabPlayer> optionalTabPlayer = plugin.getTabList().getTabPlayer(p);
            if (optionalTabPlayer.isEmpty()) {
                return;
            }

            final TabPlayer targetTabPlayer = optionalTabPlayer.get();

            // Prevent duplicate packets
            if (roles.contains(role)) {
                return;
            }
            roles.add(role);

            // Send packet
            final Nametag tag = nametags.get(role);
            if (tag != null) {
                dispatchCreatePacket(plugin, targetTabPlayer, role, tag, tabPlayer, p.getUsername());
            }
        });
    }

    private void dispatchGroupCreatePacket(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer,
                                           @NotNull String teamName, @NotNull Nametag nametag,
                                           @NotNull String... teamMembers) {
        if (!teams) {
            return;
        }
        tabPlayer.getGroup().getTabPlayers(plugin, tabPlayer).forEach(viewer -> {
            if (!viewer.getPlayer().isActive()) {
                return;
            }


            dispatchCreatePacket(plugin, tabPlayer, teamName, nametag, viewer, teamMembers);
        });
    }

    private void dispatchCreatePacket(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer,
                                      @NotNull String teamName, @NotNull Nametag nametag,
                                      @NotNull TabPlayer viewer,
                                      @NotNull String... teamMembers) {
        if (!teams) {
            return;
        }
        final boolean canSee = plugin.getVanishManager().canSee(viewer.getPlayer().getUsername(), tabPlayer.getPlayer().getUsername());
        if (!canSee) {
            return;
        }

        final UpdateTeamsPacket packet = UpdateTeamsPacket.create(plugin, tabPlayer, teamName, nametag, viewer, teamMembers);
        trackedTeams.put(viewer.getPlayer().getUniqueId(), teamName);
        final boolean isNameTagEmpty = tabPlayer.getGroup().nametag().isEmpty() && !plugin.getSettings().isRemoveNametags();
        sendPacket(viewer.getPlayer(), packet, isNameTagEmpty);
    }

    private void dispatchGroupChangePacket(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer,
                                           @NotNull String teamName,
                                           @NotNull Nametag nametag) {
        if (!teams) {
            return;
        }
        final boolean isNameTagEmpty = tabPlayer.getGroup().nametag().isEmpty() && !plugin.getSettings().isRemoveNametags();
        tabPlayer.getGroup().getTabPlayers(plugin, tabPlayer).forEach(viewer -> {
            if (viewer == tabPlayer || !viewer.getPlayer().isActive()) {
                return;
            }

            final boolean canSee = plugin.getVanishManager().canSee(viewer.getPlayer().getUsername(), tabPlayer.getPlayer().getUsername());
            if (!canSee) {
                return;
            }

            // Prevent sending change nametag packets to players who are not tracking the team
            if (!trackedTeams.containsEntry(viewer.getPlayer().getUniqueId(), teamName)) {
                return;
            }

            final UpdateTeamsPacket packet = UpdateTeamsPacket.changeNametag(plugin, tabPlayer, teamName, viewer, nametag);
            final Component prefix = packet.prefix();
            final Component suffix = packet.suffix();
            final Optional<Component[]> cached = tabPlayer.getRelationalNametag(viewer.getPlayer().getUniqueId());
            // Skip if the nametag is the same as the cached one
            if (cached.isPresent() && cached.get()[0].equals(prefix) && cached.get()[1].equals(suffix)) {
                return;
            }
            tabPlayer.setRelationalNametag(viewer.getPlayer().getUniqueId(), prefix, suffix);
            sendPacket(viewer.getPlayer(), packet, isNameTagEmpty);
        });
    }

    private void dispatchGroupPacket(@NotNull UpdateTeamsPacket packet, @NotNull Group group) {
        if (!teams) {
            return;
        }

        final boolean isRemove = packet.isRemoveTeam();
        final boolean isNameTagEmpty = group.nametag().isEmpty();
        group.registeredServers(plugin).forEach(server -> server.getPlayersConnected().forEach(connected -> {
            try {
                sendPacket(connected, packet, isNameTagEmpty);
                if (isRemove) {
                    trackedTeams.remove(connected.getUniqueId(), packet.teamName());
                }
            } catch (Throwable e) {
                plugin.log(Level.ERROR, "Failed to dispatch packet (unsupported client or server version)", e);
            }
        }));
    }

    private void dispatchGroupPacket(@NotNull UpdateTeamsPacket packet, @NotNull TabPlayer tabPlayer) {
        if (!teams) {
            return;
        }
        final Player player = tabPlayer.getPlayer();
        final Optional<ServerConnection> optionalServerConnection = player.getCurrentServer();
        if (optionalServerConnection.isEmpty()) {
            return;
        }

        final Set<Player> players = tabPlayer.getGroup().getPlayers(plugin);
        final boolean isNameTagEmpty = tabPlayer.getGroup().nametag().isEmpty() && !plugin.getSettings().isRemoveNametags();
        players.forEach(connected -> {
            try {
                final boolean canSee = plugin.getVanishManager().canSee(connected.getUsername(), player.getUsername());
                if (!canSee) {
                    return;
                }

                sendPacket(connected, packet, isNameTagEmpty);
            } catch (Throwable e) {
                plugin.log(Level.ERROR, "Failed to dispatch packet (unsupported client or server version)", e);
            }
        });
    }

    private void sendPacket(@NotNull Player player, @NotNull UpdateTeamsPacket packet, boolean isNameTagEmpty) {
        if (!player.isActive()) {
            plugin.getTabList().removeOfflinePlayer(player);
            return;
        }
        if (player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_21_2) && isNameTagEmpty) {
            return;
        }

        final ConnectedPlayer connectedPlayer = (ConnectedPlayer) player;
        connectedPlayer.getConnection().write(packet);
    }

    public void registerPacket() {
        if (!teams) {
            return;
        }
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
                    .mapping(0x5A, MINECRAFT_1_19_4, false)
                    .mapping(0x5C, MINECRAFT_1_20_2, false)
                    .mapping(0x5E, MINECRAFT_1_20_3, false)
                    .mapping(0x60, MINECRAFT_1_20_5, false)
                    .mapping(0x67, MINECRAFT_1_21_4, false);
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
        if (!teams) {
            return;
        }
        final Player player = tabPlayer.getPlayer();
        final String team = createdTeams.get(target.getPlayer().getUniqueId());
        if (team == null) {
            return;
        }

        final UpdateTeamsPacket removeTeam = UpdateTeamsPacket.removeTeam(plugin, team);
        final boolean isNameTagEmpty = tabPlayer.getGroup().nametag().isEmpty() && !plugin.getSettings().isRemoveNametags();
        sendPacket(player, removeTeam, isNameTagEmpty);
        trackedTeams.remove(player.getUniqueId(), team);

        if (canSee) {
            final Nametag tag = nametags.get(team);
            if (tag != null) {
                dispatchCreatePacket(plugin, tabPlayer, team, tag, target, target.getPlayer().getUsername());
            }
        }
    }

}
