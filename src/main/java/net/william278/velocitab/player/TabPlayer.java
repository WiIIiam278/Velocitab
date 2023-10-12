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
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class TabPlayer implements Comparable<TabPlayer> {
    private final Player player;
    private final Role role;
    @Getter
    private int headerIndex = 0;
    @Getter
    private int footerIndex = 0;
    @Getter
    private Component lastDisplayname;
    private String teamName;

    public TabPlayer(@NotNull Player player, @NotNull Role role) {
        this.player = player;
        this.role = role;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Role getRole() {
        return role;
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
                .orElse("unknown");
    }

    /**
     * Get the TAB server group this player is connected to
     *
     * @param plugin instance of the {@link Velocitab} plugin
     * @return the name of the server group the player is on
     */
    @NotNull
    public String getServerGroup(@NotNull Velocitab plugin) {
        return plugin.getSettings().getServerGroup(this.getServerName());
    }

    /**
     * Get the ordinal position of the TAB server group this player is connected to
     *
     * @param plugin instance of the {@link Velocitab} plugin
     * @return The ordinal position of the server group
     */
    public int getServerGroupPosition(@NotNull Velocitab plugin) {
        return plugin.getSettings().getServerGroupPosition(getServerGroup(plugin));
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
        final String serverGroup = plugin.getSettings().getServerGroup(getServerName());
        return Placeholder.replace(plugin.getSettings().getFormat(serverGroup), plugin, this)
                .thenApply(formatted -> plugin.getFormatter().format(formatted, this, plugin))
                .thenApply(c -> this.lastDisplayname = c);
    }

    @NotNull
    public CompletableFuture<String> getNametag(@NotNull Velocitab plugin) {
        final String serverGroup = plugin.getSettings().getServerGroup(getServerName());
        return Placeholder.replace(plugin.getSettings().getNametag(serverGroup), plugin, this)
                .thenApply(formatted -> plugin.getFormatter().formatLegacySymbols(formatted, this, plugin));

    }

    @NotNull
    public CompletableFuture<String> getTeamName(@NotNull Velocitab plugin) {
        if (!plugin.getSettings().isSortPlayers()) {
            return CompletableFuture.completedFuture("");
        }

        final String sortingFormat = String.join("", plugin.getSettings().getSortingElements());
        return Placeholder.replace(sortingFormat, plugin, this) // Replace placeholders
                .thenApply(formatted -> formatted.length() > 12 ? formatted.substring(0, 12) : formatted) // Truncate
                .thenApply(truncated -> truncated + getPlayer().getUniqueId().toString().substring(0, 4)) // Make unique
                .thenApply(teamName -> this.teamName = teamName)
                .exceptionally(e -> {
                    plugin.log(Level.ERROR, "Failed to get team name for " + player.getUsername(), e);
                    return "";
                });
    }

    public Optional<String> getLastTeamName() {
        return Optional.ofNullable(teamName);
    }


    public void sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        tabList.getHeader(this).thenAccept(header -> tabList.getFooter(this)
                .thenAccept(footer -> player.sendPlayerListHeaderAndFooter(header, footer)));
    }

    public void incrementHeaderIndex(@NotNull Velocitab plugin) {
        headerIndex++;
        if (headerIndex >= plugin.getSettings().getHeaderListSize(getServerGroup(plugin))) {
            headerIndex = 0;
        }
    }

    public void incrementFooterIndex(@NotNull Velocitab plugin) {
        footerIndex++;
        if (footerIndex >= plugin.getSettings().getFooterListSize(getServerGroup(plugin))) {
            footerIndex = 0;
        }
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
