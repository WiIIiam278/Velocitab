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

package net.william278.velocitab.sorting;

import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SortingManager {

    private final Velocitab plugin;
    private static final String DELIMITER = ":::";

    public SortingManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public CompletableFuture<String> getTeamName(@NotNull TabPlayer player) {
        if (!plugin.getSettings().isSortPlayers()) {
            return CompletableFuture.completedFuture("");
        }

        return Placeholder.replace(String.join(DELIMITER, player.getGroup().sortingPlaceholders()), plugin, player)
                .thenApply(s -> Arrays.asList(s.split(DELIMITER)))
                .thenApply(v -> v.stream().map(this::adaptValue).collect(Collectors.toList()))
                .thenApply(v -> handleList(player, v));
    }

    @NotNull
    private String handleList(@NotNull TabPlayer player, @NotNull List<String> values) {
        String result = String.join("", values);

        if (result.length() > 12) {
            result = result.substring(0, 12);
            plugin.log(Level.WARN, "Sorting element list is too long, truncating to 16 characters");
        }

        result += player.getPlayer().getUniqueId().toString().substring(0, 4); // Make unique

        return result;
    }

    @NotNull
    private String adaptValue(@NotNull String value) {
        if (value.isEmpty()) {
            return "";
        }

        if (value.matches("^-?[0-9]\\d*(\\.\\d+)?$")) {
            double parsed = Double.parseDouble(value);
            parsed = Math.max(0, parsed);
            return compressNumber(Integer.MAX_VALUE / 4d - parsed);
        }

        if (value.length() > 6) {
            return value.substring(0, 4);
        }

        return value;
    }

    @NotNull
    public String compressNumber(double number) {
        int wholePart = (int) number;
        final char decimalChar = (char) ((number - wholePart) * Character.MAX_VALUE);
        final List<Character> charList = new ArrayList<>();

        while (wholePart > 0) {
            char digit = (char) (wholePart % Character.MAX_VALUE);

            charList.add(0, digit);

            wholePart /= Character.MAX_VALUE;
        }

        if (charList.isEmpty()) {
            charList.add((char) 0);
        }
        return charList.stream().map(String::valueOf).collect(Collectors.joining()) + decimalChar;
    }
}

