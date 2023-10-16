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

package net.william278.velocitab.config;

import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Different formatting markup options for the TAB list
 */
@SuppressWarnings("unused")
public enum Formatter {
    MINEDOWN(
            (text, player, plugin) -> new MineDown(text).toComponent(),
            (text) -> text.replace("__", "_\\_"),
            "MineDown"
    ),
    MINIMESSAGE(
            (text, player, plugin) -> plugin.getMiniPlaceholdersHook()
                    .map(hook -> hook.format(text, player.getPlayer()))
                    .orElse(MiniMessage.miniMessage().deserialize(parseSections(text))),
            (text) -> MiniMessage.miniMessage().escapeTags(text),
            "MiniMessage"
    ),
    LEGACY(
            (text, player, plugin) -> LegacyComponentSerializer.legacyAmpersand().deserialize(text),
            Function.identity(),
            "Legacy Text"
    );

    /**
     * Name of the formatter
     */
    private final String name;

    /**
     * Function to apply formatting to a string
     */
    private final TriFunction<String, TabPlayer, Velocitab, Component> formatter;
    /**
     * Function to escape formatting characters in a string
     */
    private final Function<String, String> escaper;

    Formatter(@NotNull TriFunction<String, TabPlayer, Velocitab, Component> formatter, @NotNull Function<String, String> escaper,
              @NotNull String name) {
        this.formatter = formatter;
        this.escaper = escaper;
        this.name = name;
    }

    @NotNull
    public Component format(@NotNull String text, @NotNull TabPlayer player, @NotNull Velocitab plugin) {
        return formatter.apply(text, player, plugin);
    }

    @NotNull
    public String formatLegacySymbols(@NotNull String text, @NotNull TabPlayer player, @NotNull Velocitab plugin) {
        return LegacyComponentSerializer.legacySection()
                .serialize(format(text, player, plugin));
    }
    @NotNull
    public String escape(@NotNull String text) {
        return escaper.apply(text);
    }

    @NotNull
    public String getName() {
        return name;
    }

    private static String replaceAmpersandCodesWithSection(String text) {
        char[] b = text.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx#".indexOf(b[i + 1]) > -1) {
                b[i] = '§';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    public static String parseSections(String text) {
        String value = MiniMessage.miniMessage().serialize(
                        LegacyComponentSerializer.legacySection().deserialize(
                                replaceAmpersandCodesWithSection(text)))
                .replace("\\<", "<");
        return MiniMessage.miniMessage().serialize(
                        LegacyComponentSerializer.legacySection().deserialize(
                                replaceAmpersandCodesWithSection(text)))
                .replace("\\<", "<");
    }

}
