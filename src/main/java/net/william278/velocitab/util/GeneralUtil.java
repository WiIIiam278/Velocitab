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

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralUtil {

    private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{4})");

    @NotNull
    public static String unescapeJava(@NotNull String input) {
        final StringBuilder output = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < length) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case 'b' -> { output.append('\b'); i++; }
                    case 't' -> { output.append('\t'); i++; }
                    case 'n' -> { output.append('\n'); i++; }
                    case 'f' -> { output.append('\f'); i++; }
                    case 'r' -> { output.append('\r'); i++; }
                    case '\"' -> { output.append('\"'); i++; }
                    case '\'' -> { output.append('\''); i++; }
                    case '\\' -> { output.append('\\'); i++; }
                    case 'u' -> {
                        if (i + 5 < length) {
                            String hex = input.substring(i + 2, i + 6);
                            try {
                                int unicode = Integer.parseInt(hex, 16);
                                output.append((char) unicode);
                                i += 5;
                            } catch (NumberFormatException e) {
                                output.append("\\u").append(hex);
                                i += 5;
                            }
                        } else {
                            output.append(c);
                        }
                    }
                    default -> {
                        output.append(c).append(next);
                        i++;
                    }
                }
            } else {
                output.append(c);
            }
        }

        return replaceUnicodeEscapes(output.toString());
    }

    @NotNull
    private static String replaceUnicodeEscapes(@NotNull String input) {
        final Matcher matcher = UNICODE_PATTERN.matcher(input);
        final StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            final String unicodeStr = matcher.group(1);
            final int unicode = Integer.parseInt(unicodeStr, 16);
            matcher.appendReplacement(result, Character.toString(unicode));
        }
        matcher.appendTail(result);

        return result.toString();
    }

}
