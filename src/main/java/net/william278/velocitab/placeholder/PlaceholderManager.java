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
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderManager {

    private static final Pattern PLACEHOLDER_PATTERN = Placeholder.PLACEHOLDER_PATTERN;
    private final static String ELSE_PLACEHOLDER = "ELSE";

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

        placeholders.forEach(placeholder -> Placeholder.replaceSingle(placeholder, plugin, tabPlayer)
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
        return Placeholder.processRelationalPlaceholders(applied, plugin);
    }

    @NotNull
    public String applyPlaceholders(@NotNull TabPlayer player, @NotNull String text, @NotNull TabPlayer target) {
        final Map<String, String> parsed = placeholders.computeIfAbsent(player.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        final String applied = applyPlaceholderReplacements(text, player, parsed);
        final String relational = Placeholder.processRelationalPlaceholders(applied, plugin);

        final Map<String, String> targetParsed = placeholders.computeIfAbsent(target.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        final String targetApplied = applyPlaceholderReplacements(relational.replace("%target_", "%"), target, targetParsed);
        return Placeholder.processRelationalPlaceholders(targetApplied, plugin);
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
}
