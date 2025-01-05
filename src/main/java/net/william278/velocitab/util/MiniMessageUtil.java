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

package net.william278.velocitab.util;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniMessageUtil {

    @Getter
    private static final MiniMessageUtil INSTANCE = new MiniMessageUtil();

    private final Pattern legacyRGBPattern = Pattern.compile("#&[0-9a-fA-F]{6}");
    private final Pattern legacyPattern = Pattern.compile("&[0-9a-fA-F]");
    private final Pattern legacySectionPattern = Pattern.compile("§[0-9a-fA-F]");
    private int errorsCount;
    
    private MiniMessageUtil() {
        errorsCount = 0;
    }

    @NotNull
    public String checkForErrors(@NotNull String text, @NotNull Velocitab plugin) {
        final List<String> errors = Lists.newArrayList();
        String copy = text;
        copy = processLegacySections(errors, copy, legacyRGBPattern);
        copy = processLegacySections(errors, copy, legacyPattern);
        copy = processLegacySections(errors, copy, legacySectionPattern);

        if (errorsCount > 0 && errorsCount % 10 == 0) {
            errorsCount++;
            plugin.log(Level.WARN, "Found legacy formatting which is not supported if the formatter is set to MINIMESSAGE." +
                    " Remove the following characters from your config or make sure placeholders don't contain them: " + errors + ". & and § are replaced with * to prevent issues with MINIMESSAGE.");
            if(errorsCount > 100000) {
                errorsCount = 0;
            }
        }

        return copy;
    }

    @NotNull
    private String processLegacySections(@NotNull List<String> errors, @NotNull String copy, @NotNull Pattern legacySectionPattern) {
        final StringBuilder result = new StringBuilder();
        final Matcher legacySectionMatcher = legacySectionPattern.matcher(copy);

        while (legacySectionMatcher.find()) {
            errors.add(legacySectionMatcher.group());
            String matched = legacySectionMatcher.group();
            String replaced = "*" + matched.substring(1);
            legacySectionMatcher.appendReplacement(result, Matcher.quoteReplacement(replaced));
            errorsCount++;
        }

        legacySectionMatcher.appendTail(result);
        return result.toString();
    }

}
