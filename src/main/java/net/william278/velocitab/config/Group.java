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

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.Nametag;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public record Group(
        String name,
        List<String> headers,
        List<String> footers,
        String format,
        Nametag nametag,
        List<String> servers,
        List<String> sortingPlaceholders,
        boolean collisions,
        int headerFooterUpdateRate,
        int placeholderUpdateRate
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

    @NotNull
    public List<RegisteredServer> registeredServers(Velocitab plugin) {
        if (isDefault() && plugin.getSettings().isFallbackEnabled()) {
            return new ArrayList<>(plugin.getServer().getAllServers());
        }
        return servers.stream()
                .map(plugin.getServer()::getServer)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public boolean isDefault() {
        return name.equals("default");
    }

    @NotNull
    public List<Player> getPlayers(Velocitab plugin) {
        List<Player> players = new ArrayList<>();
        for (RegisteredServer server : registeredServers(plugin)) {
            players.addAll(server.getPlayersConnected());
        }
        return players;
    }

    @NotNull
    public List<TabPlayer> getTabPlayers(Velocitab plugin) {
        return plugin.getTabList().getPlayers()
                .values()
                .stream()
                .filter(tabPlayer -> tabPlayer.getGroup().equals(this))
                .toList();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Group group)) {
            return false;
        }
        return name.equals(group.name);
    }
}
