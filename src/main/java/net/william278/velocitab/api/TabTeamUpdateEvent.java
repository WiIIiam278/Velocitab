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

import com.velocitypowered.api.proxy.Player;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when Velocitab is about to send a scoreboard team (nametag) packet for a player.
 * <p>
 * Listeners may modify {@link #setPrefix(Component)}, {@link #setSuffix(Component)},
 * and/or {@link #setDisplayName(Component)} to transform the components before the
 * packet is sent (e.g. to resolve custom font glyphs).
 * This event is a pure transformation hook and is not cancellable.
 *
 * @since 1.6.9
 */
@Getter
@SuppressWarnings("unused")
public class TabTeamUpdateEvent {

    @NotNull
    private final TabPlayer player;
    @NotNull
    private final Player viewer;
    @Setter
    @Nullable
    private Component prefix;
    @Setter
    @Nullable
    private Component suffix;
    @Setter
    @Nullable
    private Component displayName;
    @NotNull
    private final Mode mode;

    /**
     * Creates a new {@link TabTeamUpdateEvent}.
     *
     * @param player      the player whose nametag is being updated
     * @param viewer      the player who will receive the team packet
     * @param prefix      the nametag prefix component, or null if not applicable
     * @param suffix      the nametag suffix component, or null if not applicable
     * @param displayName the team display name component, or null if not applicable
     * @param mode        whether this is a new team creation or an update to an existing team
     */
    public TabTeamUpdateEvent(@NotNull TabPlayer player, @NotNull Player viewer,
                              @Nullable Component prefix, @Nullable Component suffix,
                              @Nullable Component displayName, @NotNull Mode mode) {
        this.player = player;
        this.viewer = viewer;
        this.prefix = prefix;
        this.suffix = suffix;
        this.displayName = displayName;
        this.mode = mode;
    }

    /**
     * Indicates whether this team packet is creating a new team or updating an existing one.
     *
     * @since 1.6.9
     */
    public enum Mode {
        /**
         * A new scoreboard team is being created for this player.
         */
        CREATE,
        /**
         * An existing scoreboard team is being updated for this player.
         */
        UPDATE
    }

}
