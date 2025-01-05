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
import net.william278.velocitab.util.MiniMessageUtil;
import net.william278.velocitab.util.QuadFunction;
import net.william278.velocitab.util.SerializationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Different formatting markup options for the TAB list
 */
@SuppressWarnings("unused")
public enum Formatter {
    MINEDOWN(
            (text, player, viewer, plugin) -> new MineDown(text).toComponent(),
            MineDown::escape,
            "MineDown",
            (text) -> new MineDown(text).toComponent(),
            (text) -> {
                throw new UnsupportedOperationException("MineDown does not support serialization");
            }
    ),
    MINIMESSAGE(
            (text, player, viewer, plugin) -> plugin.getMiniPlaceholdersHook()
                    .filter(hook -> player != null)
                    .map(hook -> hook.format(MiniMessageUtil.getINSTANCE().checkForErrors(text, plugin),
                            player.getPlayer(), viewer == null ? null : viewer.getPlayer()))
                    .orElse(MiniMessage.miniMessage().deserialize(MiniMessageUtil.getINSTANCE().checkForErrors(text, plugin))),
            (text) -> MiniMessage.miniMessage().escapeTags(text),
            "MiniMessage",
            (text) -> MiniMessage.miniMessage().deserialize(text),
            MiniMessage.miniMessage()::serialize
    ),
    LEGACY(
            (text, player, viewer, plugin) -> SerializationUtil.LEGACY_SERIALIZER.deserialize(text),
            Function.identity(),
            "Legacy Text",
            SerializationUtil.LEGACY_SERIALIZER::deserialize,
            SerializationUtil.LEGACY_SERIALIZER::serialize
    );


    /**
     * Name of the formatter
     */
    private final String name;

    /**
     * Function to apply formatting to a string
     */
    private final QuadFunction<String, TabPlayer, TabPlayer, Velocitab, Component> formatter;
    /**
     * Function to escape formatting characters in a string
     */
    private final Function<String, String> escaper;
    private final Function<String, Component> emptyFormatter;
    private final Function<Component, String> serializer;

    Formatter(@NotNull QuadFunction<String, TabPlayer, TabPlayer, Velocitab, Component> formatter, @NotNull Function<String, String> escaper,
              @NotNull String name, @NotNull Function<String, Component> emptyFormatter, @NotNull Function<Component, String> serializer) {
        this.formatter = formatter;
        this.escaper = escaper;
        this.name = name;
        this.emptyFormatter = emptyFormatter;
        this.serializer = serializer;
    }

    /**
     * Formats the given text using a specific formatter.
     *
     * @param text      The text to format
     * @param player    The TabPlayer object representing the player
     * @param tabPlayer The TabPlayer object representing the viewer (can be null)
     * @param plugin    The Velocitab plugin instance
     * @return The formatted Component object
     * @throws NullPointerException if any of the parameters (text, player, plugin) is null
     */
    @NotNull
    public Component format(@NotNull String text, @NotNull TabPlayer player, @Nullable TabPlayer tabPlayer, @NotNull Velocitab plugin) {
        return formatter.apply(text, player, tabPlayer, plugin);
    }

    /**
     * Formats the given text using a specific formatter.
     *
     * @param text   The text to format
     * @param player The TabPlayer object representing the player
     * @param plugin The Velocitab plugin instance
     * @return The formatted Component object
     * @throws NullPointerException if any of the parameters (text, player, plugin) is null
     */
    @NotNull
    public Component format(@NotNull String text, @NotNull TabPlayer player, @NotNull Velocitab plugin) {
        return formatter.apply(text, player, null, plugin);
    }

    @NotNull
    public String formatLegacySymbols(@NotNull String text, @NotNull TabPlayer player, @NotNull Velocitab plugin) {
        return LegacyComponentSerializer.legacySection()
                .serialize(format(text, player, plugin));
    }

    @NotNull
    public Component deserialize(@NotNull String text) {
        return emptyFormatter.apply(text);
    }

    @NotNull
    public String escape(@NotNull String text) {
        return escaper.apply(text);
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String serialize(@NotNull Component component) {
        return serializer.apply(component);
    }

}
