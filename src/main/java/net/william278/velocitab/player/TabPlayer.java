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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Formatter;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.packet.UpdateTeamsPacket;
import net.william278.velocitab.tab.Nametag;
import net.william278.velocitab.tab.PlayerTabList;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@ToString
public final class TabPlayer implements Comparable<TabPlayer> {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\w+)%");
    private static final String PLACEHOLDER_DELIMITER = "<-DELIMITER->";

    private final Velocitab plugin;
    private final Player player;
    @Setter
    private Role role;
    private int headerIndex = 0;
    private int footerIndex = 0;
    // Each TabPlayer contains the components for each TabPlayer it's currently viewing this player
    private final Map<UUID, Component> relationalDisplayNames;
    private final Map<UUID, Component[]> relationalNametags;
    private final Map<String, String> cachedPlaceholders;
    private String lastDisplayName;
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

    public TabPlayer(@NotNull Velocitab plugin, @NotNull Player player,
                     @NotNull Role role, @NotNull Group group) {
        this.plugin = plugin;
        this.player = player;
        this.role = role;
        this.group = group;
        this.relationalDisplayNames = Maps.newConcurrentMap();
        this.relationalNametags = Maps.newConcurrentMap();
        this.cachedPlaceholders = Maps.newConcurrentMap();
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
    public CompletableFuture<String> getDisplayName(@NotNull Velocitab plugin) {
        final String format = formatGroup();
        return Placeholder.replace(format, plugin, this)
                .thenApply(d -> cacheDisplayName(d, format));
    }

    @NotNull
    private String formatGroup() {
        final Set<String> placeholders = Sets.newHashSet();
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(group.format());
        while (matcher.find()) {
            placeholders.add("%" + matcher.group(1) + "%");
        }

        return String.join(PLACEHOLDER_DELIMITER, placeholders);
    }

    @NotNull
    private String cacheDisplayName(@NotNull String placeholders, @NotNull String keys) {
        String displayName = group.format();
        final String[] placeholderArray = placeholders.split(PLACEHOLDER_DELIMITER);
        final String[] keyArray = keys.split(PLACEHOLDER_DELIMITER);

        for (int i = 0; i < placeholderArray.length; i++) {
            final String placeholder = keyArray[i];
            final String value = placeholderArray[i];
            cachedPlaceholders.put(placeholder, value);
            displayName = displayName.replace(placeholder, value);
        }

        displayName = displayName.replace("\n", "");
        final boolean isMiniMessage = plugin.getFormatter().equals(Formatter.MINIMESSAGE);
        if (isMiniMessage) {
            displayName = Formatter.LEGACY.serialize(MiniMessage.miniMessage().deserialize(displayName));
        }
        displayName = Placeholder.replaceInternal(displayName, plugin, this);
        if (isMiniMessage) {
            displayName = MiniMessage.miniMessage().serialize(Formatter.LEGACY.deserialize(displayName))
                    .replace("\\<", "<");
        }
        return lastDisplayName = displayName;
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
        return tabList.getHeader(this).thenCompose(header -> tabList.getFooter(this).thenAccept(footer -> {
            final boolean disabled = plugin.getSettings().isDisableHeaderFooterIfEmpty();
            if (disabled) {
                if (!Component.empty().equals(header) && !header.equals(lastHeader)) {
                    lastHeader = header;
                    player.sendPlayerListHeader(header);
                }
                if (!Component.empty().equals(footer) && !footer.equals(lastFooter)) {
                    lastFooter = footer;
                    player.sendPlayerListFooter(footer);
                }
            } else {
                if (!header.equals(lastHeader)) {
                    lastHeader = header;
                    player.sendPlayerListHeader(header);
                }
                if (!footer.equals(lastFooter)) {
                    lastFooter = footer;
                    player.sendPlayerListFooter(footer);
                }
            }
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

    public void setRelationalDisplayName(@NotNull UUID target, @NotNull Component displayName) {
        relationalDisplayNames.put(target, displayName);
    }

    public void unsetRelationalDisplayName(@NotNull UUID target) {
        relationalDisplayNames.remove(target);
    }

    public Optional<Component> getRelationalDisplayName(@NotNull UUID target) {
        return Optional.ofNullable(relationalDisplayNames.get(target));
    }

    public void setRelationalNametag(@NotNull UUID target, @NotNull Component prefix, @NotNull Component suffix) {
        relationalNametags.put(target, new Component[]{prefix, suffix});
    }

    public void unsetRelationalNametag(@NotNull UUID target) {
        relationalNametags.remove(target);
    }

    public Optional<Component[]> getRelationalNametag(@NotNull UUID target) {
        return Optional.ofNullable(relationalNametags.get(target));
    }

    public void clearCachedData() {
        loaded = false;
        relationalDisplayNames.clear();
        relationalNametags.clear();
        lastHeader = null;
        lastFooter = null;
        role = Role.DEFAULT_ROLE;
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

    public Optional<String> getCachedPlaceholderValue(@NotNull String placeholder) {
        return Optional.ofNullable(cachedPlaceholders.get(placeholder));
    }
}
