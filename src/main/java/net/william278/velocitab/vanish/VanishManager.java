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

package net.william278.velocitab.vanish;

import com.velocitypowered.api.proxy.Player;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class VanishManager {

    private final Velocitab plugin;
    private VanishIntegration integration;

    public VanishManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        setIntegration(new DefaultVanishIntegration());
    }

    public void setIntegration(@NotNull VanishIntegration integration) {
        this.integration = integration;
    }

    @NotNull
    public VanishIntegration getIntegration() {
        return integration;
    }

    public boolean canSee(@NotNull String name, @NotNull String otherName) {
        return integration.canSee(name, otherName);
    }

    public boolean isVanished(@NotNull String name) {
        return integration.isVanished(name);
    }

    public void vanishPlayer(@NotNull Player player) {
        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
        if (tabPlayer.isEmpty()) {
            return;
        }

        plugin.getTabList().vanishPlayer(tabPlayer.get());
        plugin.getScoreboardManager().ifPresent(scoreboardManager -> scoreboardManager.vanishPlayer(player));
    }

    public void unVanishPlayer(@NotNull Player player) {
        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
        if (tabPlayer.isEmpty()) {
            return;
        }

        plugin.getTabList().unVanishPlayer(tabPlayer.get());
        plugin.getScoreboardManager().ifPresent(scoreboardManager -> scoreboardManager.unVanishPlayer(player));
    }
}
