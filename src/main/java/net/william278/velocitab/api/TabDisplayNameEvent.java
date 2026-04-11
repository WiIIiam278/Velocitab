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
 * Fired when Velocitab is about to set a player's display name in the tab list.
 * <p>
 * Listeners may modify {@link #setDisplayName(Component)} to transform the component
 * before it is sent (e.g. to resolve custom font glyphs).
 * This event is a pure transformation hook and is not cancellable.
 *
 * @since 1.6.9
 */
@Getter
@SuppressWarnings("unused")
public class TabDisplayNameEvent {

    @NotNull
    private final TabPlayer player;
    @NotNull
    private final TabPlayer viewer;
    @Setter
    @NotNull
    private Component displayName;

    /**
     * Creates a new {@link TabDisplayNameEvent}.
     *
     * @param player      the player whose display name is being set
     * @param viewer      the player who will see this tab list entry
     * @param displayName the computed display name component
     */
    public TabDisplayNameEvent(@NotNull TabPlayer player, @NotNull TabPlayer viewer,
                               @NotNull Component displayName) {
        this.player = player;
        this.viewer = viewer;
        this.displayName = displayName;
    }

}
