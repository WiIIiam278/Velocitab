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

import net.jodah.expiringmap.ExpiringMap;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mvel2.MVEL;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionManager {

    private static final String VELOCITAB_REL_CONDITION = "velocitab_rel_condition:";
    private static final String VELOCITAB_CONDITION = "velocitab_condition:";
    private static final String VELOCITAB_REL_PLACEHOLDER_PERM = "velocitab_rel_perm:";
    private static final String VELOCITAB_REL_WHO_IS_SEEING = "velocitab_rel_who-is-seeing";
    private static final String VELOCITAB_REL_VANISH = "velocitab_rel_vanish";

    private final Velocitab plugin;
    private final Pattern targetPlaceholderPattern;
    private final Pattern miniEscapeEndTags;
    private final Map<String, Object> cachedExpressions;

    private static final Map<String, String> REPLACE_CHARS = Map.of(
            "?dp?", ":"
    );

    public ConditionManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.targetPlaceholderPattern = Pattern.compile("%target_(\\w+)?%");
        this.miniEscapeEndTags = Pattern.compile("</(\\w+)>");
        this.cachedExpressions = ExpiringMap.builder()
                .expiration(5, TimeUnit.MINUTES)
                .build();
    }

    @NotNull
    public String checkConditions(@NotNull TabPlayer target, @NotNull String argument) {
        final List<String> parameters = collectParameters(argument);
        if (parameters.isEmpty()) {
            plugin.getLogger().warn("Empty condition");
            return "";
        }

        String condition = parameters.get(0);
        if (parameters.size() < 3) {
            plugin.getLogger().warn("Invalid condition: Missing true/false values for condition: {}", condition);
            return "";
        }

        condition = plugin.getPlaceholderManager().applyPlaceholders(target, condition);
        final String falseValue = processFalseValue(parameters.get(2));
        final String expression = buildExpression(condition);
        return evaluateAndFormatCondition(expression, target, parameters.get(1), falseValue);
    }

    @NotNull
    private List<String> collectParameters(@NotNull String argument) {
        for (Map.Entry<String, String> entry : REPLACE_CHARS.entrySet()) {
            argument = argument.replace(entry.getKey(), entry.getValue());
        }

        return Arrays.stream(argument.split(":"))
                .map(s -> s.replace("''", "\""))
                .toList();
    }

    @NotNull
    private String processFalseValue(@NotNull String falseValue) {
        final Matcher matcher = miniEscapeEndTags.matcher(falseValue);
        if (matcher.find()) {
            final String tag = matcher.group(1);
            if (falseValue.startsWith("</" + tag + ">")) {
                falseValue = falseValue.substring(tag.length() + 3);
            }
        }
        return falseValue;
    }

    @NotNull
    private String buildExpression(@NotNull String condition) {
        return condition.replace("and", "&&").replace("or", "||")
                .replace("AND", "&&").replace("OR", "||");
    }

    @NotNull
    private String evaluateAndFormatCondition(@NotNull String expression, @NotNull TabPlayer target,
                                              @NotNull String trueValue, @NotNull String falseValue) {
        final String targetString = parseTargetPlaceholders(expression, target).trim();
        try {
            final Object result = evaluateExpression(targetString);
            if (result instanceof Boolean) {
                final boolean boolResult = (Boolean) result;
                return boolResult ? trueValue : falseValue;
            }
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to evaluate condition: {} error: {}", expression, e.getMessage());
        }
        return "";
    }

    @NotNull
    private Object evaluateExpression(@NotNull String expression) {
        return cachedExpressions.computeIfAbsent(expression, MVEL::eval);
    }

    @NotNull
    private String parseTargetPlaceholders(@NotNull String input, @NotNull TabPlayer target) {
        return targetPlaceholderPattern.matcher(input).replaceAll(match -> {
            final String placeholder = match.group(1);
            if (placeholder == null) {
                return "";
            }

            final String text = "%" + placeholder + "%";
            final Optional<String> placeholderValue = plugin.getPlaceholderManager().getCachedPlaceholderValue(text, target.getPlayer().getUniqueId());
            return placeholderValue.orElse(text);
        });
    }

    public String handleVelocitabPlaceholders(@NotNull String text, @NotNull TabPlayer player, @Nullable TabPlayer viewer) {
        if (viewer == null) {
            return handleConditionPlaceholders(text, player);
        }

        return handleRelPlaceholders(text, player, viewer);
    }

    @NotNull
    private String handleRelPlaceholders(@NotNull String text, @NotNull TabPlayer player, @NotNull TabPlayer viewer) {
        switch (text) {
            case VELOCITAB_REL_WHO_IS_SEEING -> viewer.getPlayer().getUsername();
            case VELOCITAB_REL_VANISH -> {
                if (plugin.getVanishManager().isVanished(viewer.getPlayer().getUsername())) {
                    return "true";
                }

                return "false";
            }
        }

        if (text.length() < VELOCITAB_REL_CONDITION.length()) {
            return text;
        }

        if (text.startsWith(VELOCITAB_REL_CONDITION)) {
            return checkConditions(player, text.substring(VELOCITAB_REL_CONDITION.length()));
        }

        if (text.startsWith(VELOCITAB_REL_PLACEHOLDER_PERM)) {
            final String cleaned = text.substring(VELOCITAB_REL_PLACEHOLDER_PERM.length());
            final int firstSeparator = cleaned.indexOf(':');
            if (firstSeparator == -1) {
                return "";
            }

            final String permission = cleaned.substring(0, firstSeparator);
            final String trueValue = cleaned.substring(firstSeparator + 1);
            return viewer.getPlayer().hasPermission(permission) ? trueValue : "";
        }

        return text;
    }

    @NotNull
    private String handleConditionPlaceholders(@NotNull String text, @NotNull TabPlayer player) {
        if (text.startsWith(VELOCITAB_CONDITION)) {
            return checkConditions(player, text.substring(VELOCITAB_CONDITION.length()));
        }

        return text;
    }
}
