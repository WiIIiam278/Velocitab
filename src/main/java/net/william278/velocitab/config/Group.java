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

package net.william278.velocitab.config;

import com.google.common.collect.Sets;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.Nametag;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public record Group(
        String name,
        List<String> headers,
        List<String> footers,
        String format,
        Nametag nametag,
        Set<String> servers,
        List<String> sortingPlaceholders,
        Map<String, List<PlaceholderReplacement>> placeholderReplacements,
        boolean collisions,
        int headerFooterUpdateRate,
        int placeholderUpdateRate,
        boolean onlyListPlayersInSameServer
) {

    @NotNull
    public String getHeader(int index) {
        return headers.isEmpty() ? "" : StringEscapeUtils.unescapeJava(headers
                .get(Math.max(0, Math.min(index, headers.size() - 1))));
    }

    @NotNull
    public String getFooter(int index) {
        return footers.isEmpty() ? "" : StringEscapeUtils.unescapeJava(footers
                .get(Math.max(0, Math.min(index, footers.size() - 1))));
    }

    public boolean containsServer(@NotNull Velocitab plugin, @NotNull String serverName) {
        return registeredServers(plugin).stream()
                .anyMatch(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(serverName));
    }

    @NotNull
    public Set<RegisteredServer> registeredServers(@NotNull Velocitab plugin) {
        return registeredServers(plugin, true);
    }

    @NotNull
    public Set<RegisteredServer> registeredServers(@NotNull Velocitab plugin, boolean includeAllPlayers) {
        if ((includeAllPlayers && plugin.getSettings().isShowAllPlayersFromAllGroups()) ||
                (isDefault(plugin) && plugin.getSettings().isFallbackEnabled())) {
            return Sets.newHashSet(plugin.getServer().getAllServers());
        }
        return getRegexServers(plugin);
    }

    @NotNull
    private Set<RegisteredServer> getRegexServers(@NotNull Velocitab plugin) {
        final Set<RegisteredServer> totalServers = Sets.newHashSet();
        for (String server : servers) {
            try {
                final Matcher matcher = Pattern.compile(server, Pattern.CASE_INSENSITIVE).matcher("");
                plugin.getServer().getAllServers().stream()
                        .filter(registeredServer -> matcher.reset(registeredServer.getServerInfo().getName()).matches())
                        .forEach(totalServers::add);
            } catch (PatternSyntaxException exception) {
                plugin.log(Level.WARN, "Invalid regex pattern " + server + " in group " + name, exception);
                plugin.getServer().getServer(server).ifPresent(totalServers::add);
            }
        }
        return totalServers;
    }

    public boolean isDefault(@NotNull Velocitab plugin) {
        return name.equals(plugin.getSettings().getFallbackGroup());
    }

    @NotNull
    public Set<Player> getPlayers(@NotNull Velocitab plugin) {
        Set<Player> players = Sets.newHashSet();
        for (RegisteredServer server : registeredServers(plugin)) {
            players.addAll(server.getPlayersConnected());
        }
        return players;
    }

    @NotNull
    public Set<Player> getPlayers(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer) {
        if (plugin.getSettings().isShowAllPlayersFromAllGroups()) {
            return Sets.newHashSet(plugin.getServer().getAllPlayers());
        }
        if (onlyListPlayersInSameServer) {
            return tabPlayer.getPlayer().getCurrentServer()
                    .map(s -> Sets.newHashSet(s.getServer().getPlayersConnected()))
                    .orElseGet(Sets::newHashSet);
        }
        return getPlayers(plugin);
    }

    /**
     * Retrieves the set of TabPlayers associated with the given Velocitab plugin instance.
     * If the plugin is configured to show all players from all groups, all players will be returned.
     *
     * @param plugin The Velocitab plugin instance.
     * @return A set of TabPlayers.
     */
    @NotNull
    public Set<TabPlayer> getTabPlayers(@NotNull Velocitab plugin) {
        if (plugin.getSettings().isShowAllPlayersFromAllGroups()) {
            return Sets.newHashSet(plugin.getTabList().getPlayers().values());
        }
        return plugin.getTabList().getPlayers()
                .values()
                .stream()
                .filter(tabPlayer -> tabPlayer.getGroup().equals(this))
                .collect(Collectors.toSet());
    }

    @NotNull
    public Set<TabPlayer> getTabPlayers(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer) {
        if (plugin.getSettings().isShowAllPlayersFromAllGroups()) {
            return Sets.newHashSet(plugin.getTabList().getPlayers().values());
        }
        if (onlyListPlayersInSameServer) {
            return plugin.getTabList().getPlayers()
                    .values()
                    .stream()
                    .filter(player -> player.getGroup().equals(this) && player.getServerName().equals(tabPlayer.getServerName()))
                    .collect(Collectors.toSet());
        }
        return getTabPlayers(plugin);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Group group)) {
            return false;
        }
        return name.equals(group.name);
    }
}
