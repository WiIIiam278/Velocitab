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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.velocitypowered.api.proxy.Player;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Formatter;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.hook.miniconditions.MiniConditionManager;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderManager {

    private static final String ELSE_PLACEHOLDER = "ELSE";
    private static final Pattern VELOCITAB_PATTERN = Pattern.compile("<velocitab_.*?>");
    private static final Pattern TEST = Pattern.compile("<.*?>");
    private static final Pattern CONDITION_REPLACER = Pattern.compile("<velocitab_rel_condition:[^:]*:");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%.*?%", Pattern.DOTALL);
    private static final Map<String, String> SYMBOL_SUBSTITUTES = Map.of(
            "<", "*LESS*",
            ">", "*GREATER*"
    );
    private static final Map<String, String> SYMBOL_SUBSTITUTES_2 = Map.of(
            "*LESS*", "*LESS2*",
            "*GREATER*", "*GREATER2*"
    );
    private static final String VEL_PLACEHOLDER = "<vel";
    private static final String VELOCITAB_PLACEHOLDER = "<velocitab_rel";

    private final Velocitab plugin;
    private final Map<UUID, Map<String, String>> placeholders;

    public PlaceholderManager(Velocitab plugin) {
        this.plugin = plugin;
        this.placeholders = Maps.newConcurrentMap();
    }

    public void fetchPlaceholders(@NotNull Group group) {
        final List<String> texts = group.getTextsWithPlaceholders();
        group.getPlayersAsList(plugin).forEach(player -> fetchPlaceholders(player.getUniqueId(), texts));
    }

    public void fetchPlaceholders(@NotNull UUID uuid, @NotNull List<String> texts) {
        final Player player = plugin.getServer().getPlayer(uuid).orElse(null);
        if (player == null) {
            return;
        }
        final Map<String, String> parsed = placeholders.computeIfAbsent(uuid, k -> Maps.newConcurrentMap());
        final TabPlayer tabPlayer = plugin.getTabList().getTabPlayer(player)
                .orElse(new TabPlayer(plugin, player, Role.DEFAULT_ROLE, plugin.getTabList().getGroupOrDefault(player)));

        final List<String> placeholders = texts.stream()
                .map(PlaceholderManager::extractPlaceholders)
                .flatMap(List::stream)
                .map(s -> s.replace("%target_", "%"))
                .toList();

        placeholders.forEach(placeholder -> replaceSingle(placeholder, plugin, tabPlayer)
                .ifPresentOrElse(replacement -> parsed.put(placeholder, replacement),
                        () -> plugin.getPAPIProxyBridgeHook().ifPresent(hook ->
                                hook.formatPlaceholders(placeholder, player).thenAccept(replacement -> {
                                    if(replacement == null || replacement.equals(placeholder)) {
                                        return;
                                    }
                                    parsed.put(placeholder, replacement);
                                }))));
    }

    @NotNull
    public String applyPlaceholders(@NotNull TabPlayer player, @NotNull String text) {
        final Map<String, String> parsed = placeholders.computeIfAbsent(player.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        final String applied = applyPlaceholderReplacements(text, player, parsed);
        return processRelationalPlaceholders(applied, plugin);
    }

    @NotNull
    public String applyPlaceholders(@NotNull TabPlayer player, @NotNull String text, @NotNull TabPlayer target) {
        final Map<String, String> parsed = placeholders.computeIfAbsent(player.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        final String applied = applyPlaceholderReplacements(text, player, parsed);
        final String relational = processRelationalPlaceholders(applied, plugin);

        final Map<String, String> targetParsed = placeholders.computeIfAbsent(target.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        final String targetApplied = applyPlaceholderReplacements(relational.replace("%target_", "%"), target, targetParsed);
        return processRelationalPlaceholders(targetApplied, plugin);
    }

    public void clearPlaceholders(@NotNull UUID uuid) {
        placeholders.remove(uuid);
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

    @NotNull
    private String applyPlaceholderReplacements(@NotNull String text, @NotNull TabPlayer player,
                                                         @NotNull Map<String, String> parsed) {
        for (final Map.Entry<String, List<PlaceholderReplacement>> entry : player.getGroup().placeholderReplacements().entrySet()) {
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


    @NotNull
    private String applyPlaceholders(@NotNull String text, @NotNull Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    public Optional<String> getCachedPlaceholderValue(@NotNull String text, @NotNull UUID uuid) {
        if (!placeholders.containsKey(uuid)) {
            return Optional.empty();
        }

        return Optional.ofNullable(placeholders.get(uuid).get(text));
    }

    private Optional<String> replaceSingle(@NotNull String placeholder, @NotNull Velocitab plugin, @NotNull TabPlayer player) {
        final Optional<Placeholder> optionalPlaceholder = Placeholder.byName(placeholder);
        if (optionalPlaceholder.isEmpty()) {
            //check if it's parameterised
            for (Placeholder placeholderType : Placeholder.getPARAMETERISED()) {
                final Matcher matcher = placeholderType.getPattern().matcher(placeholder);
                if (matcher.find()) {
                    final String s = StringUtils.chop(matcher.group().replace("%" + placeholderType.name().toLowerCase(), "")
                            .replaceFirst("_", ""));
                    return Optional.of(placeholderType.getReplacer().apply(s, plugin, player));
                }
            }

            return Optional.empty();
        }

        if(optionalPlaceholder.get().isParameterised()) {
            throw new IllegalArgumentException("Placeholder " + placeholder + " is parameterised");
        }

        final Placeholder placeholderType = optionalPlaceholder.get();
        return Optional.of(placeholderType.getReplacer().apply(null, plugin, player));
    }

    private String processRelationalPlaceholders(@NotNull String format, @NotNull Velocitab plugin) {
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
}
