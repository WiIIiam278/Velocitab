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
import com.google.common.collect.Sets;
import com.velocitypowered.api.proxy.Player;
import lombok.Setter;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PlaceholderManager {

    @Setter
    private boolean debug = false;

    private static final String ELSE_PLACEHOLDER = "ELSE";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%.*?%", Pattern.DOTALL);
    private static final Pattern VELOCITAB_PLACEHOLDERS = Pattern.compile("<velocitab[^<>]*(?:<(?!v)[^<>]*>[^<>]*)*>");
    private static final Pattern VELOCITAB_REL_PLACEHOLDERS = Pattern.compile("<velocitab_rel[^<>]*(?:<(?!v)[^<>]*>[^<>]*)*>");

    private final Velocitab plugin;
    private final Map<UUID, Map<String, String>> placeholders;
    private final Map<UUID, Set<CompletableFuture<?>>> requests;
    private final Map<Group, List<String>> cachedTexts;
    private final Set<UUID> blocked;
    private final ConditionManager conditionManager;
    private Map<Group, Map<String, Map<String, String>>> placeholdersReplacements;

    public PlaceholderManager(Velocitab plugin) {
        this.plugin = plugin;
        this.placeholders = Maps.newConcurrentMap();
        this.requests = Maps.newConcurrentMap();
        this.blocked = Sets.newConcurrentHashSet();
        this.cachedTexts = Maps.newConcurrentMap();
        this.conditionManager = new ConditionManager(plugin);
        this.placeholdersReplacements = Maps.newConcurrentMap();
        this.preparePlaceholdersReplacements();
    }

    public void preparePlaceholdersReplacements() {
        placeholdersReplacements = Maps.newConcurrentMap();
        for (Group group : plugin.getTabGroupsManager().getGroups()) {
            final Map<String, Map<String, String>> map = Maps.newHashMap();
            placeholdersReplacements.put(group, map);
            for (String placeholder : group.placeholderReplacements().keySet()) {
                final Map<String, String> repMap = Maps.newHashMap();
                map.put(placeholder, repMap);
                for (PlaceholderReplacement replacement : group.placeholderReplacements().get(placeholder)) {
                    repMap.put(replacement.placeholder(), replacement.replacement());
                }
            }
        }
    }

    public void fetchPlaceholders(@NotNull Group group) {
        final List<String> texts = cachedTexts.computeIfAbsent(group, Group::getTextsWithPlaceholders);
        group.getPlayersAsList(plugin).forEach(player -> fetchPlaceholders(player.getUniqueId(), texts, group));
    }

    public void reload() {
        cachedTexts.clear();
    }

    public void fetchPlaceholders(@NotNull UUID uuid, @NotNull List<String> texts, @NotNull Group group) {
        final Player player = plugin.getServer().getPlayer(uuid).orElse(null);
        if (player == null) {
            return;
        }

        if (blocked.contains(uuid)) {
            return;
        }

        final Map<String, String> parsed = placeholders.computeIfAbsent(uuid, k -> Maps.newConcurrentMap());
        final TabPlayer tabPlayer = plugin.getTabList().getTabPlayer(player)
                .orElse(new TabPlayer(plugin, player,
                        plugin.getLuckPermsHook().map(hook -> hook.getPlayerRole(player)).orElse(Role.DEFAULT_ROLE),
                        plugin.getTabList().getGroupOrDefault(player)));

        final List<String> placeholders = texts.stream()
                .map(PlaceholderManager::extractPlaceholders)
                .flatMap(List::stream)
                .map(s -> s.replace("%target_", "%"))
                .toList();

        final long start = System.currentTimeMillis();

        placeholders.forEach(placeholder -> replaceSingle(placeholder, plugin, tabPlayer)
                .ifPresentOrElse(replacement -> parsed.put(placeholder, replacement),
                        () -> plugin.getPAPIProxyBridgeHook().ifPresent(hook -> {
                            final CompletableFuture<String> future = hook.formatPlaceholders(placeholder, player);
                            requests.computeIfAbsent(player.getUniqueId(), u -> Sets.newConcurrentHashSet()).add(future);
                            future.thenAccept(replacement -> {
                                if (replacement == null || replacement.equals(placeholder)) {
                                    return;
                                }

                                if (blocked.contains(player.getUniqueId())) {
                                    return;
                                }

                                if (debug) {
                                    plugin.getLogger().info("Placeholder {} replaced with  {} in {}ms", placeholder, replacement, System.currentTimeMillis() - start);
                                }

                                final long diff = System.currentTimeMillis() - start;
                                if (diff > group.placeholderUpdateRate()) {
                                    final long increase = diff + 100;
                                    plugin.getLogger().warn("""
                                                    Placeholder {} took more than group placeholder update rate of {} ms to update. This may cause a thread leak.
                                                    Please fix the issue of the plugin providing the placeholder.
                                                    If you can't fix it, increase the placeholder update rate of the group to at least {} ms.
                                                    """
                                            , placeholder, group.placeholderUpdateRate(), increase);
                                }

                                parsed.put(placeholder, replacement);
                                requests.get(player.getUniqueId()).remove(future);
                            });
                        })));
    }

    @NotNull
    public String applyPlaceholders(@NotNull TabPlayer player, @NotNull String text) {
        final Map<String, String> parsed = placeholders.computeIfAbsent(player.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        return applyPlaceholderReplacements(text, player, parsed);
    }

    @NotNull
    public String applyPlaceholders(@NotNull TabPlayer player, @NotNull String text, @NotNull TabPlayer viewer) {
        final Map<String, String> parsed = placeholders.computeIfAbsent(player.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        final String applied = applyPlaceholderReplacements(text, player, parsed);

        final Map<String, String> targetParsed = placeholders.computeIfAbsent(viewer.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        return applyPlaceholderReplacements(applied.replace("%target_", "%"), viewer, targetParsed);
    }

    @NotNull
    public String applyViewerPlaceholders(@NotNull TabPlayer viewer, @NotNull String text) {
        final Map<String, String> parsed = placeholders.computeIfAbsent(viewer.getPlayer().getUniqueId(), uuid -> Maps.newConcurrentMap());
        return applyPlaceholderReplacements(text.replace("%target_", "%"), viewer, parsed);
    }

    public void clearPlaceholders(@NotNull UUID uuid) {
        blocked.add(uuid);
        placeholders.remove(uuid);
        Optional.ofNullable(requests.get(uuid)).ifPresent(set -> set.forEach(c -> c.cancel(true)));
    }

    public void unblockPlayer(@NotNull UUID uuid) {
        blocked.remove(uuid);
        requests.remove(uuid);
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


    @Nullable
    private String getReplacement(@NotNull Group group, @NotNull String placeholder, @NotNull String text) {
        final Map<String, Map<String, String>> replacements = placeholdersReplacements.get(group);
        if (replacements == null) {
            return null;
        }
        final Map<String, String> replacementMap = replacements.get(placeholder);
        if (replacementMap == null) {
            return null;
        }
        final String replacement = replacementMap.get(text);
        if (replacement == null) {
            return replacementMap.get(ELSE_PLACEHOLDER);
        }

        return replacement;
    }

    @NotNull
    private String applyPlaceholderReplacements(@NotNull String text, @NotNull TabPlayer player,
                                                @NotNull Map<String, String> parsed) {
        for (final Map.Entry<String, List<PlaceholderReplacement>> entry : player.getGroup().placeholderReplacements().entrySet()) {
            final String replaced = parsed.get(entry.getKey());
            final String replacement = getReplacement(player.getGroup(), entry.getKey(), replaced);
            if (replacement != null) {
                text = text.replace(entry.getKey(), replacement);
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
                final java.util.regex.Matcher matcher = placeholderType.getPattern().matcher(placeholder);
                if (matcher.find()) {
                    final String s = StringUtils.chop(matcher.group().replace("%" + placeholderType.name().toLowerCase(), "")
                            .replaceFirst("_", ""));
                    return Optional.of(placeholderType.getReplacer().apply(s, plugin, player));
                }
            }

            return Optional.empty();
        }

        if (optionalPlaceholder.get().isParameterised()) {
            throw new IllegalArgumentException("Placeholder " + placeholder + " is parameterised");
        }

        final Placeholder placeholderType = optionalPlaceholder.get();
        return Optional.of(placeholderType.getReplacer().apply(null, plugin, player));
    }

    @NotNull
    public String formatVelocitabPlaceholders(@NotNull String text, @NotNull TabPlayer player, @Nullable TabPlayer viewer) {
        final Matcher matcher = VELOCITAB_PLACEHOLDERS.matcher(text);
        if (!matcher.find()) {
            return text;
        }

        final StringBuilder result = new StringBuilder(text.length());
        int lastEnd = 0;

        do {
            String placeholder = matcher.group();
            String cleanedPlaceholder = placeholder.substring(1, placeholder.length() - 1);

            String replacement;
            try {
                replacement = conditionManager.handleVelocitabPlaceholders(cleanedPlaceholder, player, viewer);
                if (replacement.equals(cleanedPlaceholder)) {
                    continue;
                }
            } catch (Exception e) {
                plugin.getLogger().warn("Failed to calculate condition for {}", cleanedPlaceholder, e);
                replacement = placeholder;
            }

            result.append(text, lastEnd, matcher.start()).append(replacement);
            lastEnd = matcher.end();
        } while (matcher.find());

        result.append(text.substring(lastEnd));
        return result.toString();
    }

    @NotNull
    public String stripVelocitabRelPlaceholders(@NotNull String text) {
        final Matcher matcher = VELOCITAB_REL_PLACEHOLDERS.matcher(text);
        final StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            String placeholder = matcher.group();
            String cleanedPlaceholder = placeholder.substring(1, placeholder.length() - 1);
            try {
                String replacement = "";
                result.append(text, lastEnd, matcher.start()).append(replacement);
                lastEnd = matcher.end();
            } catch (Exception e) {
                plugin.getLogger().warn("Failed to calculate condition for {}", cleanedPlaceholder, e);
            }
        }
        result.append(text.substring(lastEnd));
        return result.toString();
    }
}
