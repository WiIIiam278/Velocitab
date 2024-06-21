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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.hook.MiniPlaceholdersHook;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniConditionManager {

    private final Velocitab plugin;
    private final JexlEngine jexlEngine;
    private final JexlContext jexlContext;
    private final Pattern targetPlaceholderPattern = Pattern.compile("%target_(\\w+)?%");
    private final Pattern miniEscapeEndTags = Pattern.compile("</(\\w+)>");

    public MiniConditionManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.jexlEngine = createJexlEngine();
        this.jexlContext = createJexlContext();
    }

    @NotNull
    private JexlEngine createJexlEngine() {
        return new JexlBuilder()
                .create();
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
        final List<String> parameters = Lists.newArrayList();
        while (queue.hasNext()) {
            parameters.add(queue.pop().value());
        }

        if (parameters.isEmpty()) {
            plugin.getLogger().warn("Empty condition");
            return Component.empty();
        }

        String condition = parameters.get(0);
        condition = condition.replace("?lt;", "<").replace("?gt;", ">");
        for (final Map.Entry<String, String> entry : MiniPlaceholdersHook.REPLACE.entrySet()) {
            condition = condition.replace(entry.getValue(), entry.getKey());
            condition = condition.replace(entry.getKey()+entry.getKey(), entry.getKey());
        }

        for (final Map.Entry<String, String> entry : Placeholder.CONDITIONAL_SUBSTITUTES.entrySet()) {
            condition = condition.replace(entry.getValue(), entry.getKey());
        }

        if (parameters.size() < 3) {
            plugin.getLogger().warn("Invalid condition: Missing true/false values for condition: {}", condition);
            return Component.empty();
        }

        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(target);
        if (tabPlayer.isEmpty()) {
            return Component.empty();
        }

        condition = Placeholder.replaceInternal(condition, plugin, tabPlayer.get());

        final String trueValue = replaceString(parameters.get(1));
        String falseValue = replaceString(parameters.get(2));
        final Matcher matcher = miniEscapeEndTags.matcher(falseValue);
        if (matcher.find()) {
            final String tag = matcher.group(1);
            //remove the tag from the start of the string if it is at the start
            if (falseValue.startsWith("</" + tag + ">")) {
                falseValue = falseValue.substring(tag.length() + 3);
            }
        }
        final String expression = condition.replace("and", "&&").replace("or", "||")
                .replace("AND", "&&").replace("OR", "||");
        final String targetString = parseTargetPlaceholders(expression, target);
        try {
            final Object result = jexlEngine.createExpression(targetString).evaluate(jexlContext);

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

    @NotNull
    private String replaceString(@NotNull String string) {
        return string.replace("?lt;", "<").replace("?gt;", ">")
                .replace("?dp?", ":");
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
