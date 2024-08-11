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

package net.william278.velocitab.hook.miniconditions;

import com.google.common.collect.Lists;
import com.velocitypowered.api.proxy.Player;
import net.jodah.expiringmap.ExpiringMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniConditionManager {

    public final static Map<String, String> REPLACE = Map.of(
            "\"", "-q-",
            "'", "-a-"
    );

    public final static Map<String, String> REPLACE_2 = Map.of(
            "*LESS3*", "<",
            "*GREATER3*",">",
            "*LESS2*", "<",
            "*GREATER2*", ">"
            );

    private final static Map<String, String> REPLACE_3 = Map.of(
            "?dp?", ":"
    );

    private final Velocitab plugin;
    private final JexlEngine jexlEngine;
    private final JexlContext jexlContext;
    private final Pattern targetPlaceholderPattern;
    private final Pattern miniEscapeEndTags;
    private final Map<String, Object> cachedExpressions;

    public MiniConditionManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.jexlEngine = createJexlEngine();
        this.jexlContext = createJexlContext();
        this.targetPlaceholderPattern = Pattern.compile("%target_(\\w+)?%");
        this.miniEscapeEndTags = Pattern.compile("</(\\w+)>");
        this.cachedExpressions = ExpiringMap.builder()
                .expiration(5, TimeUnit.MINUTES)
                .build();
    }

    @NotNull
    private JexlEngine createJexlEngine() {
        return new JexlBuilder().create();
    }

    @NotNull
    private JexlContext createJexlContext() {
        final JexlContext jexlContext = new MapContext();
        jexlContext.set("startsWith", new StartsWith());
        jexlContext.set("endsWith", new EndsWith());
        return jexlContext;
    }

    @NotNull
    public Component checkConditions(@NotNull Player target, @NotNull Player audience, @NotNull ArgumentQueue queue) {
        final List<String> parameters = collectParameters(queue);
        if (parameters.isEmpty()) {
            plugin.getLogger().warn("Empty condition");
            return Component.empty();
        }


        String condition = decodeCondition(parameters.get(0));
        if (parameters.size() < 3) {
            plugin.getLogger().warn("Invalid condition: Missing true/false values for condition: {}", condition);
            return Component.empty();
        }

        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(target);
        if (tabPlayer.isEmpty()) {
            return Component.empty();
        }


        condition = Placeholder.replaceInternal(condition, plugin, tabPlayer.get());
        final String falseValue = processFalseValue(parameters.get(2));
        final String expression = buildExpression(condition);
        return evaluateAndFormatCondition(expression, target, audience, parameters.get(1), falseValue);
    }

    @NotNull
    private List<String> collectParameters(@NotNull ArgumentQueue queue) {
        final List<String> parameters = Lists.newArrayList();
        while (queue.hasNext()) {
            String param = queue.pop().value();
            for (Map.Entry<String, String> entry : REPLACE_2.entrySet()) {
                param = param.replace(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, String> entry : REPLACE_3.entrySet()) {
                param = param.replace(entry.getKey(), entry.getValue());
            }
            parameters.add(param);
        }
        return parameters;
    }

    @NotNull
    private String decodeCondition(@NotNull String condition) {
        for (Map.Entry<String, String> entry : REPLACE.entrySet()) {
            condition = condition.replace(entry.getValue(), entry.getKey());
            condition = condition.replace(entry.getKey() + entry.getKey(), entry.getKey());
        }
        for (Map.Entry<String, String> entry : REPLACE_2.entrySet()) {
            condition = condition.replace(entry.getValue(), entry.getKey());
        }
        return condition;
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
    private Component evaluateAndFormatCondition(@NotNull String expression, @NotNull Player target, @NotNull Player audience, @NotNull String trueValue, @NotNull String falseValue) {
        final String targetString = parseTargetPlaceholders(expression, target);
        try {
            final Object result = evaluateExpression(targetString);
            if (result instanceof Boolean) {
                final boolean boolResult = (Boolean) result;
                final String value = boolResult ? trueValue : falseValue;
                return plugin.getMiniPlaceholdersHook().orElseThrow().format(value, target, audience);
            }
        } catch (Exception e) {
            plugin.getLogger().warn("Failed to evaluate condition: {} error: {}", expression, e.getMessage());
        }
        return Component.empty();
    }

    @NotNull
    private Object evaluateExpression(@NotNull String expression) {
        return cachedExpressions.computeIfAbsent(expression, key -> jexlEngine.createExpression(key).evaluate(jexlContext));
    }

    @NotNull
    private String parseTargetPlaceholders(@NotNull String input, @NotNull Player target) {
        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(target);
        if (tabPlayer.isEmpty()) {
            return input;
        }

        return targetPlaceholderPattern.matcher(input).replaceAll(match -> {
            final String placeholder = match.group(1);
            if (placeholder == null) {
                return "";
            }

            final String text = "%" + placeholder + "%";
            final Optional<String> placeholderValue = tabPlayer.get().getCachedPlaceholderValue(text);
            return placeholderValue.orElse(text);
        });
    }

    @SuppressWarnings("unused")
    private static class StartsWith {
        public boolean startsWith(String str, String prefix) {
            return str != null && str.startsWith(prefix);
        }
    }

    @SuppressWarnings("unused")
    private static class EndsWith {
        public boolean endsWith(String str, String suffix) {
            return str != null && str.endsWith(suffix);
        }
    }

}
