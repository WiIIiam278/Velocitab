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

import com.velocitypowered.api.proxy.Player;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VanishTabList {
    
    private final Velocitab plugin;
    private final PlayerTabList tabList;
    
    public VanishTabList(Velocitab plugin) {
        this.plugin = plugin;
        this.tabList = plugin.getTabList();
    }


    public void vanishPlayer(@NotNull TabPlayer tabPlayer) {
        tabList.getPlayers().values().forEach(p -> {
            if (p.getPlayer().equals(tabPlayer.getPlayer())) {
                return;
            }

            if (!plugin.getVanishManager().canSee(p.getPlayer().getUsername(), tabPlayer.getPlayer().getUsername())) {
                p.getPlayer().getTabList().removeEntry(tabPlayer.getPlayer().getUniqueId());
            }
        });
    }

    public void unVanishPlayer(@NotNull TabPlayer tabPlayer) {
        final UUID uuid = tabPlayer.getPlayer().getUniqueId();

        tabPlayer.getDisplayName(plugin).thenAccept(c -> tabList.getPlayers().values().forEach(p -> {
            if (p.getPlayer().equals(tabPlayer.getPlayer())) {
                return;
            }

            if (!p.getPlayer().getTabList().containsEntry(uuid)) {
                p.getPlayer().getTabList().addEntry(tabList.createEntry(tabPlayer, p.getPlayer().getTabList(), c));
            } else {
                p.getPlayer().getTabList().getEntry(uuid).ifPresent(entry -> entry.setDisplayName(c));
            }
        }));

    }

    /**
     * Recalculates the visibility of players in the tab list for the given player.
     * If tabPlayer can see the player, the player will be added to the tab list.
     *
     * @param tabPlayer The TabPlayer object representing the player for whom to recalculate the tab list visibility.
     */
    public void recalculateVanishForPlayer(@NotNull TabPlayer tabPlayer) {
        final Player player = tabPlayer.getPlayer();
        final List<String> serversInGroup = tabPlayer.getGroup().servers();

        plugin.getServer().getAllPlayers().forEach(p -> {
            if (p.equals(player)) {
                return;
            }

            final Optional<TabPlayer> targetOptional = tabList.getTabPlayer(p);
            if (targetOptional.isEmpty()) {
                return;
            }

            final TabPlayer target = targetOptional.get();
            final String serverName = target.getServerName();

            if (plugin.getSettings().isOnlyListPlayersInSameGroup()
                    && !serversInGroup.contains(serverName)) {
                return;
            }

            final boolean canSee = !plugin.getVanishManager().isVanished(p.getUsername()) ||
                    plugin.getVanishManager().canSee(player.getUsername(), p.getUsername());

            if (!canSee) {
                player.getTabList().removeEntry(p.getUniqueId());
                plugin.getScoreboardManager().ifPresent(s -> s.recalculateVanishForPlayer(tabPlayer, target, false));
            } else {
                if (!player.getTabList().containsEntry(p.getUniqueId())) {
                    tabList.createEntry(target, player.getTabList()).thenAccept(e -> {
                        player.getTabList().addEntry(e);
                        plugin.getScoreboardManager().ifPresent(s -> s.recalculateVanishForPlayer(tabPlayer, target, true));
                    });
                }
            }
        });
    }
    
}
