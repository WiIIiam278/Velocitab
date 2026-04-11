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

package net.william278.velocitab.api;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when Velocitab is about to send the tab list header and footer to a player.
 * <p>
 * Listeners may modify {@link #setHeader(Component)} and/or {@link #setFooter(Component)}
 * to transform the components before they are sent (e.g. to resolve custom font glyphs).
 * This event is a pure transformation hook and is not cancellable.
 *
 * @since 1.6.9
 */
@Getter
@SuppressWarnings("unused")
public class TabHeaderFooterEvent {

    @NotNull
    private final TabPlayer player;
    @Setter
    @NotNull
    private Component header;
    @Setter
    @NotNull
    private Component footer;

    /**
     * Creates a new {@link TabHeaderFooterEvent}.
     *
     * @param player the player who is about to receive the header and footer
     * @param header the computed header component
     * @param footer the computed footer component
     */
    public TabHeaderFooterEvent(@NotNull TabPlayer player,
                                @NotNull Component header,
                                @NotNull Component footer) {
        this.player = player;
        this.header = header;
        this.footer = footer;
    }

}
