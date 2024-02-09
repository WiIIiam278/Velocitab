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

package net.william278.velocitab.player;

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.packet.UpdateTeamsPacket;
import net.william278.velocitab.tab.Nametag;
import net.william278.velocitab.tab.PlayerTabList;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
@ToString
public final class TabPlayer implements Comparable<TabPlayer> {

    private final Player player;
    @Setter
    private Role role;
    private int headerIndex = 0;
    private int footerIndex = 0;
    private Component lastDisplayName;
    private Component lastHeader;
    private Component lastFooter;
    private String teamName;
    @Nullable
    @Setter
    private UpdateTeamsPacket.TeamColor teamColor;
    @Nullable
    @Setter
    private String customName;
    @Nullable
    @Setter
    private String lastServer;
    @NotNull
    @Setter
    private Group group;
    @Setter
    private boolean loaded;

    public TabPlayer(@NotNull Player player, @NotNull Role role, @NotNull Group group) {
        this.player = player;
        this.role = role;
        this.group = group;
    }

    @NotNull
    public String getRoleWeightString() {
        return getRole().getWeightString();
    }

    /**
     * Get the server name the player is currently on.
     * Isn't affected by server aliases defined in the config.
     *
     * @return The server name
     */
    @NotNull
    public String getServerName() {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse(ObjectUtils.firstNonNull(lastServer, "unknown"));
    }

    /**
     * Get the ordinal position of the TAB server group this player is connected to
     *
     * @param plugin instance of the {@link Velocitab} plugin
     * @return The ordinal position of the server group
     */
    public int getServerGroupPosition(@NotNull Velocitab plugin) {
        return plugin.getTabGroups().getPosition(group);
    }

    /**
     * Get the display name of the server the player is currently on.
     * Affected by server aliases defined in the config.
     *
     * @param plugin The plugin instance
     * @return The display name of the server
     */
    @NotNull
    public String getServerDisplayName(@NotNull Velocitab plugin) {
        return plugin.getSettings().getServerDisplayName(getServerName());
    }

    @NotNull
    public CompletableFuture<Component> getDisplayName(@NotNull Velocitab plugin) {
        return Placeholder.replace(group.format(), plugin, this)
                .thenApply(formatted -> plugin.getFormatter().format(formatted, this, plugin))
                .thenApply(c -> this.lastDisplayName = c);
    }

    @NotNull
    public CompletableFuture<Nametag> getNametag(@NotNull Velocitab plugin) {
        return Placeholder.replace(group.nametag(), plugin, this);
    }

    @NotNull
    public CompletableFuture<String> getTeamName(@NotNull Velocitab plugin) {
        return plugin.getSortingManager().getTeamName(this)
                .thenApply(teamName -> this.teamName = teamName);
    }

    public Optional<String> getLastTeamName() {
        return Optional.ofNullable(teamName);
    }

    public CompletableFuture<Void> sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        return tabList.getHeader(this).thenCompose(header -> tabList.getFooter(this)
                .thenAccept(footer -> {
                    lastHeader = header;
                    lastFooter = footer;
                    player.sendPlayerListHeaderAndFooter(header, footer);
                }));
    }

    public void incrementIndexes() {
        incrementHeaderIndex();
        incrementFooterIndex();
    }

    public void incrementHeaderIndex() {
        headerIndex++;
        if (headerIndex >= group.headers().size()) {
            headerIndex = 0;
        }
    }

    public void incrementFooterIndex() {
        footerIndex++;
        if (footerIndex >= group.footers().size()) {
            footerIndex = 0;
        }
    }

    /**
     * Returns the custom name of the TabPlayer, if it has been set.
     *
     * @return An Optional object containing the custom name, or empty if no custom name has been set.
     */
    public Optional<String> getCustomName() {
        return Optional.ofNullable(customName);
    }

    @Override
    public int compareTo(@NotNull TabPlayer o) {
        final int roleDifference = role.compareTo(o.role);
        if (roleDifference == 0) {
            return player.getUsername().compareTo(o.player.getUsername());
        }
        return roleDifference;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TabPlayer other && player.getUniqueId().equals(other.player.getUniqueId());
    }
}
