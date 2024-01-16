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

package net.william278.velocitab.tab;

import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a nametag to be displayed above a player, with prefix and suffix
 */
public record Nametag(@NotNull String prefix, @NotNull String suffix) {

    @NotNull
    public Component getPrefixComponent(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer) {
        return plugin.getFormatter().format(prefix, tabPlayer, plugin);
    }

    @NotNull
    public Component getSuffixComponent(@NotNull Velocitab plugin, @NotNull TabPlayer tabPlayer) {
        return plugin.getFormatter().format(suffix, tabPlayer, plugin);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Nametag other)) {
            return false;
        }
        return (prefix.equals(other.prefix)) && (suffix.equals(other.suffix));
    }

}
