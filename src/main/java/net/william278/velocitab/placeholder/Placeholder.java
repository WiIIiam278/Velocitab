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

package net.william278.velocitab.placeholder;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.util.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
public enum Placeholder {

    PLAYERS_ONLINE((plugin, player) -> Integer.toString(plugin.getServer().getPlayerCount())),
    MAX_PLAYERS_ONLINE((plugin, player) -> Integer.toString(plugin.getServer().getConfiguration().getShowMaxPlayers())),
    LOCAL_PLAYERS_ONLINE((plugin, player) -> player.getPlayer().getCurrentServer()
            .map(ServerConnection::getServer)
            .map(RegisteredServer::getPlayersConnected)
            .map(players -> Integer.toString(players.size()))
            .orElse("")),
    GROUP_PLAYERS_ONLINE((param, plugin, player) -> {
        if (param.isEmpty()) {
            return Integer.toString(player.getGroup().getPlayers(plugin).size());
        }
        return plugin.getTabGroupsManager().getGroup(param)
                .map(group -> Integer.toString(group.getPlayers(plugin).size()))
                .orElse("Group " + param + " not found");
    }),
    CURRENT_DATE_DAY((plugin, player) -> DateTimeFormatter.ofPattern("dd").format(LocalDateTime.now())),
    CURRENT_DATE_WEEKDAY((param, plugin, player) -> {
        if (param.isEmpty()) {
            return DateTimeFormatter.ofPattern("EEEE").format(LocalDateTime.now());
        }

        final String countryCode = param.toUpperCase();
        final Locale locale = Locale.forLanguageTag(countryCode);
        return DateTimeFormatter.ofPattern("EEEE").withLocale(locale).format(LocalDateTime.now());
    }),
    CURRENT_DATE_MONTH((plugin, player) -> DateTimeFormatter.ofPattern("MM").format(LocalDateTime.now())),
    CURRENT_DATE_YEAR((plugin, player) -> DateTimeFormatter.ofPattern("yyyy").format(LocalDateTime.now())),
    CURRENT_DATE((param, plugin, player) -> {
        if (param.isEmpty()) {
            return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(LocalDateTime.now());
        }

        final String countryCode = param.toUpperCase();
        final Locale locale = Locale.forLanguageTag(countryCode);
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale).format(LocalDateTime.now());
    }),
    CURRENT_TIME_HOUR((plugin, player) -> DateTimeFormatter.ofPattern("HH").format(LocalDateTime.now())),
    CURRENT_TIME_MINUTE((plugin, player) -> DateTimeFormatter.ofPattern("mm").format(LocalDateTime.now())),
    CURRENT_TIME_SECOND((plugin, player) -> DateTimeFormatter.ofPattern("ss").format(LocalDateTime.now())),
    CURRENT_TIME((param, plugin, player) -> {
        if (param.isEmpty()) {
            return DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalTime.now());
        }

        final String countryCode = param.toUpperCase();
        final Locale locale = Locale.forLanguageTag(countryCode);
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(LocalTime.now());
    }),
    USERNAME((plugin, player) -> player.getCustomName().orElse(player.getPlayer().getUsername())),
    USERNAME_LOWER((plugin, player) -> player.getCustomName().orElse(player.getPlayer().getUsername()).toLowerCase()),
    SERVER((plugin, player) -> player.getServerName()),
    PING((plugin, player) -> Long.toString(player.getPlayer().getPing())),
    PREFIX((plugin, player) -> player.getRole().getPrefix()
            .orElse(getPlaceholderFallback(plugin, "%luckperms_prefix%"))),
    SUFFIX((plugin, player) -> player.getRole().getSuffix()
            .orElse(getPlaceholderFallback(plugin, "%luckperms_suffix%"))),
    ROLE((plugin, player) -> player.getRole().getName()
            .orElse(getPlaceholderFallback(plugin, "%luckperms_primary_group_name%"))),
    ROLE_DISPLAY_NAME((plugin, player) -> player.getRole().getDisplayName()
            .orElse(getPlaceholderFallback(plugin, "%luckperms_primary_group_name%"))),
    ROLE_WEIGHT((plugin, player) -> player.getRoleWeightString()
            .orElse(getPlaceholderFallback(plugin, "%luckperms_meta_weight%"))),
    SERVER_GROUP((plugin, player) -> player.getGroup().name()),
    SERVER_GROUP_INDEX((plugin, player) -> Integer.toString(player.getServerGroupPosition(plugin))),
    DEBUG_TEAM_NAME((plugin, player) -> plugin.getFormatter().escape(player.getLastTeamName().orElse(""))),
    LUCKPERMS_META((param, plugin, player) -> plugin.getLuckPermsHook()
            .map(hook -> hook.getMeta(player.getPlayer(), param))
            .orElse(getPlaceholderFallback(plugin, "%luckperms_meta_" + param + "%")));


    private static final List<Placeholder> VALUES = Arrays.asList(values());
    private static final Map<String, Placeholder> BY_NAME = VALUES.stream().collect(Collectors.toMap(p -> p.name().toLowerCase(), Function.identity()));
    @Getter
    private static final List<Placeholder> PARAMETERISED = VALUES.stream().filter(p -> p.parameterised).toList();

    /**
     * Function to replace placeholders with a real value
     */
    private final TriFunction<String, Velocitab, TabPlayer, String> replacer;
    private final boolean parameterised;
    private final Pattern pattern;

    Placeholder(@NotNull BiFunction<Velocitab, TabPlayer, String> replacer) {
        this.parameterised = false;
        this.replacer = (text, player, plugin) -> replacer.apply(player, plugin);
        this.pattern = Pattern.compile("%" + this.name().toLowerCase() + "%");
    }

    Placeholder(@NotNull TriFunction<String, Velocitab, TabPlayer, String> parameterisedReplacer) {
        this.parameterised = true;
        this.replacer = parameterisedReplacer;
        this.pattern = Pattern.compile("%" + this.name().toLowerCase() + "[^%]*%", Pattern.CASE_INSENSITIVE);
    }

    @NotNull
    private static String getPlaceholderFallback(@NotNull Velocitab plugin, @NotNull String fallback) {
        if (plugin.getPAPIProxyBridgeHook().isPresent() && plugin.getSettings().isFallbackToPapiIfPlaceholderBlank()) {
            return fallback;
        }
        return "";
    }

    public static Optional<Placeholder> byName(@NotNull String name) {
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase().replace("%", "")));
    }
}
