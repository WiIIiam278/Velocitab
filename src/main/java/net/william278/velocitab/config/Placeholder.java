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
import com.google.common.collect.Maps;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import it.unimi.dsi.fastutil.Pair;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.hook.miniconditions.MiniConditionManager;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.Nametag;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return plugin.getTabGroups().getGroup(param)
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
    ROLE_WEIGHT((plugin, player) -> player.getRoleWeightString()),
    SERVER_GROUP((plugin, player) -> player.getGroup().name()),
    SERVER_GROUP_INDEX((plugin, player) -> Integer.toString(player.getServerGroupPosition(plugin))),
    DEBUG_TEAM_NAME((plugin, player) -> plugin.getFormatter().escape(player.getLastTeamName().orElse(""))),
    LUCKPERMS_META((param, plugin, player) -> plugin.getLuckPermsHook()
            .map(hook -> hook.getMeta(player.getPlayer(), param))
            .orElse(getPlaceholderFallback(plugin, "%luckperms_meta_" + param + "%")));

    private final static Pattern VELOCITAB_PATTERN = Pattern.compile("<velocitab_.*?>");
    private final static Pattern TEST = Pattern.compile("<.*?>");
    private final static Pattern CONDITION_REPLACER = Pattern.compile("<velocitab_rel_condition:[^:]*:");
    private final static Pattern PLACEHOLDER_PATTERN = Pattern.compile("%.*?%");
    private final static String DELIMITER = ":::";
    private final static Map<String, String> SYMBOL_SUBSTITUTES = Map.of(
            "<", "*LESS*",
            ">", "*GREATER*"
    );
    private final static Map<String, String> SYMBOL_SUBSTITUTES_2 = Map.of(
            "*LESS*", "*LESS2*",
            "*GREATER*", "*GREATER2*"
    );
    private final static String VEL_PLACEHOLDER = "<vel";
    private final static String VELOCITAB_PLACEHOLDER = "<velocitab_rel";
    private final static String ELSE_PLACEHOLDER = "ELSE";

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

    public static CompletableFuture<Nametag> replace(@NotNull Nametag nametag, @NotNull Velocitab plugin,
                                                     @NotNull TabPlayer player) {
        return replace(nametag.prefix() + DELIMITER + nametag.suffix(), plugin, player)
                .thenApply(s -> s.split(DELIMITER, 2))
                .thenApply(v -> new Nametag(v[0], v.length > 1 ? v[1] : ""));
    }

    @NotNull
    private static String getPlaceholderFallback(@NotNull Velocitab plugin, @NotNull String fallback) {
        if (plugin.getPAPIProxyBridgeHook().isPresent() && plugin.getSettings().isFallbackToPapiIfPlaceholderBlank()) {
            return fallback;
        }
        return "";
    }

    @NotNull
    public static Pair<String, Map<String, String>> replaceInternal(@NotNull String format, @NotNull Velocitab plugin, @Nullable TabPlayer player) {
        format = processRelationalPlaceholders(format, plugin);
        return replacePlaceholders(format, plugin, player);
    }

    private static String processRelationalPlaceholders(@NotNull String format, @NotNull Velocitab plugin) {
        if (plugin.getFormatter().equals(Formatter.MINIMESSAGE) && format.contains(VEL_PLACEHOLDER)) {
            final Matcher conditionReplacer = CONDITION_REPLACER.matcher(format);
            while (conditionReplacer.find()) {

                final String search = conditionReplacer.group().split(":")[1];
                String condition = search;
                for (Map.Entry<String, String> entry : MiniConditionManager.REPLACE.entrySet()) {
                    condition = condition.replace(entry.getKey(), entry.getValue());
                }
                for (Map.Entry<String, String> entry : MiniConditionManager.REPLACE_2.entrySet()) {
                    condition = condition.replace(entry.getValue(), entry.getKey());
                }
                format = format.replace(search, condition);
            }

            final Matcher testMatcher = TEST.matcher(format);
            while (testMatcher.find()) {
                if (testMatcher.group().startsWith(VELOCITAB_PLACEHOLDER)) {
                    final Matcher second = TEST.matcher(testMatcher.group().substring(1));
                    while (second.find()) {
                        String s = second.group();
                        for (Map.Entry<String, String> entry : SYMBOL_SUBSTITUTES.entrySet()) {
                            s = s.replace(entry.getKey(), entry.getValue());
                        }
                        format = format.replace(second.group(), s);
                    }
                    continue;
                }
                String s = testMatcher.group();
                for (Map.Entry<String, String> entry : SYMBOL_SUBSTITUTES.entrySet()) {
                    s = s.replace(entry.getKey(), entry.getValue());
                }
                format = format.replace(testMatcher.group(), s);
            }

            final Matcher velocitabRelationalMatcher = VELOCITAB_PATTERN.matcher(format);
            while (velocitabRelationalMatcher.find()) {
                final String relationalPlaceholder = velocitabRelationalMatcher.group().substring(1, velocitabRelationalMatcher.group().length() - 1);
                String fixedString = relationalPlaceholder;
                for (Map.Entry<String, String> entry : SYMBOL_SUBSTITUTES_2.entrySet()) {
                    fixedString = fixedString.replace(entry.getKey(), entry.getValue());
                }
                format = format.replace(relationalPlaceholder, fixedString);
            }

            for (Map.Entry<String, String> entry : SYMBOL_SUBSTITUTES.entrySet()) {
                format = format.replace(entry.getValue(), entry.getKey());
            }

        }
        return format;
    }

    @NotNull
    private static Pair<String, Map<String, String>> replacePlaceholders(@NotNull String format, @NotNull Velocitab plugin,
                                                                         @Nullable TabPlayer player) {
        final Map<String, String> replacedPlaceholders = Maps.newHashMap();
        for (Placeholder placeholder : values()) {
            final Matcher matcher = placeholder.pattern.matcher(format);
            if (placeholder.parameterised) {
                format = matcher.replaceAll(matchResult -> {
                    String replacement = placeholder.replacer.apply(StringUtils.chop(matchResult.group().replace("%" + placeholder.name().toLowerCase(), "")
                            .replaceFirst("_", "")), plugin, player);
                    replacedPlaceholders.put(matchResult.group(), replacement);
                    return Matcher.quoteReplacement(replacement);
                });
            } else {
                format = matcher.replaceAll(matchResult -> {
                    String replacement = placeholder.replacer.apply(null, plugin, player);
                    replacedPlaceholders.put(matchResult.group(), replacement);
                    return Matcher.quoteReplacement(replacement);
                });
            }
        }
        return Pair.of(format, replacedPlaceholders);
    }

    @NotNull
    private static String applyPlaceholderReplacements(@NotNull String text, @NotNull TabPlayer player,
                                                       @NotNull Map<String, String> parsed) {
        for (final Map.Entry<String, List<PlaceholderReplacement>> entry : player.getGroup().placeholderReplaments().entrySet()) {
            if (!parsed.containsKey(entry.getKey())) {
                continue;
            }

            final String replaced = parsed.get(entry.getKey());
            final Optional<PlaceholderReplacement> replacement = entry.getValue().stream()
                    .filter(r -> r.placeholder().equalsIgnoreCase(replaced))
                    .findFirst();

            if (replacement.isPresent()) {
                text = text.replace(entry.getKey(), replacement.get().replacement());
            } else {
                final Optional<PlaceholderReplacement> elseReplacement = entry.getValue().stream()
                        .filter(r -> r.placeholder().equalsIgnoreCase(ELSE_PLACEHOLDER))
                        .findFirst();
                if (elseReplacement.isPresent()) {
                    text = text.replace(entry.getKey(), elseReplacement.get().replacement());
                }
            }

        }

        return applyPlaceholders(text, parsed);
    }

    public static CompletableFuture<String> replace(@NotNull String format, @NotNull Velocitab plugin,
                                                    @NotNull TabPlayer player) {

        if (format.equals(DELIMITER)) {
            return CompletableFuture.completedFuture("");
        }

        final Pair<String, Map<String, String>> replaced = replaceInternal(format, plugin, player);
        if (!PLACEHOLDER_PATTERN.matcher(replaced.first()).find()) {
            return CompletableFuture.completedFuture(replaced.first());
        }

        final List<String> placeholders = extractPlaceholders(replaced.first());
        return plugin.getPAPIProxyBridgeHook()
                .map(hook -> hook.parsePlaceholders(placeholders, player.getPlayer())
                        .exceptionally(e -> {
                            plugin.log(Level.ERROR, "An error occurred whilst parsing placeholders: " + e.getMessage());
                            return Map.of();
                        })
                )
                .orElse(CompletableFuture.completedFuture(Map.of())).exceptionally(e -> {
                    plugin.log(Level.ERROR, "An error occurred whilst parsing placeholders: " + e.getMessage());
                    return Map.of();
                })
                .thenApply(m -> applyPlaceholderReplacements(format, player, mergeMaps(m, replaced.second())));
    }

    @NotNull
    private static String applyPlaceholders(@NotNull String text, @NotNull Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    @NotNull
    private static Map<String, String> mergeMaps(@NotNull Map<String, String> map1, @NotNull Map<String, String> map2) {
        map1.putAll(map2);
        return map1;
    }

    @NotNull
    private static List<String> extractPlaceholders(@NotNull String text) {
        final List<String> placeholders = Lists.newArrayList();
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            placeholders.add(matcher.group());
        }
        return placeholders;
    }
}
