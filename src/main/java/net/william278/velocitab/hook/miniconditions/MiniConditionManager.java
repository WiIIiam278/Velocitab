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
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MiniConditionManager {

    private final Velocitab plugin;
    private final JexlEngine jexlEngine;
    private final JexlContext jexlContext;

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

        if (parameters.size() < 3) {
            return Component.empty();
        }

        String condition = parameters.get(0);

        // adventure string quote fix
        final int countQuotes = condition.length() - condition.replace("\"", "").length();
        final int countApostrophes = condition.length() - condition.replace("'", "").length();
        if (countQuotes % 2 != 0) {
            condition = "\"" + condition;
        } else if (countApostrophes % 2 != 0) {
            condition = "'" + condition;
        }

        for (final String key : Placeholder.CONDITIONAL_SUBSTITUTES.keySet()) {
            condition = condition.replace(Placeholder.CONDITIONAL_SUBSTITUTES.get(key), key);
        }
        final String trueValue = parameters.get(1);
        final String falseValue = parameters.get(2);
        final String expression = condition.replace("and", "&&").replace("or", "||");
        final Object result = jexlEngine.createExpression(expression).evaluate(jexlContext);

        if (result instanceof Boolean) {
            final boolean boolResult = (Boolean) result;
            final String value = boolResult ? trueValue : falseValue;
            return plugin.getMiniPlaceholdersHook().orElseThrow().format(value, target, audience);
        }

        return Component.empty();
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
