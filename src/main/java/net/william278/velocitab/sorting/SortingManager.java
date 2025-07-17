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

import com.google.common.collect.Lists;
import com.velocitypowered.api.network.ProtocolVersion;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SortingManager {

    private final Velocitab plugin;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?[0-9]\\d*(\\.\\d+)?$");

    public SortingManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public String getTeamName(@NotNull TabPlayer player) {
        if (!plugin.getSettings().isSortPlayers()) {
            return "";
        }

        final List<String> placeholders = player.getGroup().sortingPlaceholders()
                .stream()
                .map(s -> plugin.getPlaceholderManager().applyPlaceholders(player, s))
                .map(s -> adaptValue(s, player))
                .collect(Collectors.toList());

        return handleList(player, placeholders);
    }

    @NotNull
    private String handleList(@NotNull TabPlayer player, @NotNull List<String> values) {
        String result = String.join("", values);

        if (result.length() > 12 && isLongTeamNotAllowed(player)) {
            result = result.substring(0, 12);
        }

        final String uuid = player.getPlayer().getUniqueId().toString();
        result += uuid.substring(uuid.length() - 4); // Make unique

        return result;
    }

    private boolean isLongTeamNotAllowed(@NotNull TabPlayer player) {
        return !player.getGroup().getPlayers(plugin, player).stream()
                .allMatch(t -> t.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_18));
    }

    @NotNull
    private String adaptValue(@NotNull String value, @NotNull TabPlayer player) {
        if (value.isEmpty()) {
            return "";
        }

        if (NUMBER_PATTERN.matcher(value).matches()) {
            double parsed = Double.parseDouble(value);
            parsed = Math.max(0, parsed);
            return compressNumber(Integer.MAX_VALUE / 4d - parsed);
        }

        if (value.length() > 6 && isLongTeamNotAllowed(player)) {
            return value.substring(0, 4);
        }

        return value;
    }

    @NotNull
    public String compressNumber(double number) {
        int wholePart = (int) number;
        final char decimalChar = (char) ((number - wholePart) * Character.MAX_VALUE);
        final List<Character> charList = Lists.newArrayList();

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

