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

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public enum Placeholder {

    PLAYERS_ONLINE((plugin, player) -> Integer.toString(plugin.getServer().getPlayerCount())),
    MAX_PLAYERS_ONLINE((plugin, player) -> Integer.toString(plugin.getServer().getConfiguration().getShowMaxPlayers())),
    LOCAL_PLAYERS_ONLINE((plugin, player) -> player.getPlayer().getCurrentServer()
            .map(ServerConnection::getServer)
            .map(RegisteredServer::getPlayersConnected)
            .map(players -> Integer.toString(players.size()))
            .orElse("")),
    CURRENT_DATE((plugin, player) -> DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDateTime.now())),
    CURRENT_TIME((plugin, player) -> DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now())),
    USERNAME((plugin, player) -> plugin.getFormatter().escape(player.getCustomName().orElse(player.getPlayer().getUsername()))),
    SERVER((plugin, player) -> player.getServerDisplayName(plugin)),
    PING((plugin, player) -> Long.toString(player.getPlayer().getPing())),
    PREFIX((plugin, player) -> player.getRole().getPrefix().orElse("")),
    SUFFIX((plugin, player) -> player.getRole().getSuffix().orElse("")),
    ROLE((plugin, player) -> player.getRole().getName().orElse("")),
    ROLE_DISPLAY_NAME((plugin, player) -> player.getRole().getDisplayName().orElse("")),
    ROLE_WEIGHT((plugin, player) -> player.getRoleWeightString()),
    SERVER_GROUP((plugin, player) -> player.getServerGroup(plugin)),
    SERVER_GROUP_INDEX((plugin, player) -> Integer.toString(player.getServerGroupPosition(plugin))),
    DEBUG_TEAM_NAME((plugin, player) -> plugin.getFormatter().escape(player.getLastTeamName().orElse("")));

    /**
     * Function to replace placeholders with a real value
     */
    private final BiFunction<Velocitab, TabPlayer, String> replacer;
    private final static Pattern pattern = Pattern.compile("%.*?%");

    Placeholder(@NotNull BiFunction<Velocitab, TabPlayer, String> replacer) {
        this.replacer = replacer;
    }

    public static CompletableFuture<String> replace(@NotNull String format, @NotNull Velocitab plugin,
                                                    @NotNull TabPlayer player) {
        for (Placeholder placeholder : values()) {
            format = format.replace("%" + placeholder.name().toLowerCase() + "%", placeholder.replacer.apply(plugin, player));
        }
        final String replaced = format;

        if (!pattern.matcher(replaced).find()) {
            return CompletableFuture.completedFuture(replaced);
        }

        return plugin.getPAPIProxyBridgeHook()
                .map(hook -> hook.formatPlaceholders(replaced, player.getPlayer()))
                .orElse(CompletableFuture.completedFuture(replaced)).exceptionally(e -> {
                    plugin.log(Level.ERROR, "An error occurred whilst parsing placeholders: " + e.getMessage());
                    return replaced;
                });
    }

    @NotNull
    public static String formatSortableInt(int value, int maxValue) {
        return String.format("%0" + Integer.toString(maxValue).length() + "d", maxValue - value);
    }

}
