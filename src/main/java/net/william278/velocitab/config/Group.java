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

import com.google.common.collect.Lists;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.placeholder.PlaceholderReplacement;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.Nametag;
import net.william278.velocitab.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

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
        int formatUpdateRate,
        int nametagUpdateRate,
        int placeholderUpdateRate,
        boolean onlyListPlayersInSameServer
) {

    @NotNull
    public String getHeader(int index) {
        return headers.isEmpty() ? "" : StringUtil.unescapeJava(headers
                .get(Math.max(0, Math.min(index, headers.size() - 1))));
    }

    @NotNull
    public String getFooter(int index) {
        return footers.isEmpty() ? "" : StringUtil.unescapeJava(footers
                .get(Math.max(0, Math.min(index, footers.size() - 1))));
    }

    public boolean containsServer(@NotNull Velocitab plugin, @NotNull String serverName) {
        return registeredServers(plugin).stream()
                .anyMatch(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(serverName));
    }

    @NotNull
    public List<RegisteredServer> registeredServers(@NotNull Velocitab plugin) {
        return registeredServers(plugin, true);
    }

    @NotNull
    public List<RegisteredServer> registeredServers(@NotNull Velocitab plugin, boolean includeAllPlayers) {
        if ((includeAllPlayers && plugin.getSettings().isShowAllPlayersFromAllGroups()) ||
                (isDefault(plugin) && plugin.getSettings().isFallbackEnabled())) {
            return Lists.newArrayList(plugin.getServer().getAllServers());
        }

        return getRegexServers(plugin);
    }

    @NotNull
    private List<RegisteredServer> getRegexServers(@NotNull Velocitab plugin) {
        final Optional<List<RegisteredServer>> cachedServers = plugin.getTabGroupsManager().getCachedServers(this);
        if (cachedServers.isPresent()) {
            return cachedServers.get();
        }

        final List<RegisteredServer> totalServers = Lists.newArrayList();
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

        plugin.getTabGroupsManager().cacheServers(this, totalServers);
        return totalServers;
    }

    public boolean isDefault(@NotNull Velocitab plugin) {
        return name.equals(plugin.getSettings().getFallbackGroup());
    }

    @NotNull
    public List<Player> getPlayers(@NotNull Velocitab plugin) {
        final List<Player> players = Lists.newArrayList();
        for (RegisteredServer server : registeredServers(plugin)) {
            players.addAll(server.getPlayersConnected());
        }

        return players.stream().filter(Player::isActive).collect(Collectors.toList());
    }

    @NotNull
    public List<Player> getPlayers(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer) {
        if (plugin.getSettings().isShowAllPlayersFromAllGroups()) {
            return Lists.newArrayList(plugin.getServer().getAllPlayers());
        }

        if (onlyListPlayersInSameServer) {
            return tabPlayer.getPlayer().getCurrentServer()
                    .map(s -> Lists.newArrayList(s.getServer().getPlayersConnected()))
                    .orElseGet(Lists::newArrayList);
        }

        return getPlayers(plugin);
    }

    public List<TabPlayer> getTabPlayers(@NotNull Velocitab plugin) {
        return getTabPlayers(plugin, true);
    }

    public List<TabPlayer> getTabPlayers(@NotNull Velocitab plugin, boolean allGroups) {
        if (allGroups && plugin.getSettings().isShowAllPlayersFromAllGroups()) {
            return plugin.getTabList().getPlayers().values().stream().filter(TabPlayer::isLoaded).collect(Collectors.toList());
        }

        return plugin.getTabList().getPlayers()
                .values()
                .stream()
                .filter(tabPlayer -> tabPlayer.isLoaded() && tabPlayer.getGroup().equals(this) && tabPlayer.getPlayer().isActive())
                .collect(Collectors.toList());
    }

    @NotNull
    public List<TabPlayer> getTabPlayers(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer) {
        return getTabPlayers(plugin, tabPlayer, false);
    }

    @NotNull
    public List<TabPlayer> getTabPlayers(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer, boolean force) {
        if (plugin.getSettings().isShowAllPlayersFromAllGroups()) {
            return plugin.getTabList().getPlayers().values().stream().filter(TabPlayer::isLoaded).collect(Collectors.toList());
        }

        if (onlyListPlayersInSameServer) {
            return plugin.getTabList().getPlayers()
                    .values()
                    .stream()
                    .filter(player -> (player.isLoaded() || force) && player.getGroup().equals(this) && tabPlayer.getPlayer().isActive() && player.getServerName().equals(tabPlayer.getServerName()))
                    .collect(Collectors.toList());
        }

        return getTabPlayers(plugin);
    }

    @NotNull
    public List<String> getTextsWithPlaceholders(@NotNull Velocitab plugin) {
        final List<String> texts = Lists.newArrayList();
        texts.add(name);
        texts.add(format);
        texts.addAll(headers);
        texts.addAll(footers);
        texts.add(nametag.prefix());
        texts.add(nametag.suffix());
        texts.addAll(sortingPlaceholders);

        if (plugin.getLuckPermsHook().isEmpty()) {
            texts.add("%luckperms_meta_weight%");
        }

        return texts;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Group group)) {
            return false;
        }

        return name.equals(group.name);
    }
}
