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
import net.william278.velocitab.util.DebugSystem;
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

    public boolean canSee(@NotNull String viewer, @NotNull String target) {
        final long start = System.currentTimeMillis();
        final boolean result = integration.canSee(viewer, target);
        final long end = System.currentTimeMillis();
        if (end - start > 2) {
            DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Vanish canSee check took " + (end - start) + "ms");
        }
        return result;
    }

    public boolean isVanished(@NotNull String name) {
        final long start = System.currentTimeMillis();
        final boolean result = integration.isVanished(name);
        final long end = System.currentTimeMillis();
        if (end - start > 2) {
            DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Vanish isVanished check took " + (end - start) + "ms");
        }
        return result;
    }

    public void vanishPlayer(@NotNull Player player) {
        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
        if (tabPlayer.isEmpty()) {
            plugin.log("Failed to vanish player " + player.getUsername() + " as they are not in the tab list");
            return;
        }

        plugin.getTabList().getVanishTabList().vanishPlayer(tabPlayer.get());
        plugin.getScoreboardManager().vanishPlayer(tabPlayer.get());
    }

    public void unVanishPlayer(@NotNull Player player) {
        final Optional<TabPlayer> tabPlayer = plugin.getTabList().getTabPlayer(player);
        if (tabPlayer.isEmpty()) {
            plugin.log("Failed to un-vanish player " + player.getUsername() + " as they are not in the tab list");
            return;
        }

        plugin.getTabList().getVanishTabList().unVanishPlayer(tabPlayer.get());
        plugin.getScoreboardManager().unVanishPlayer(tabPlayer.get());
    }
}
