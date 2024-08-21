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

import com.google.common.collect.Maps;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TaskManager {

    private final Velocitab plugin;
    private final Map<Group, GroupTasks> groupTasks;

    public TaskManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.groupTasks = Maps.newConcurrentMap();
    }

    protected void cancelAllTasks() {
        groupTasks.values().forEach(GroupTasks::cancel);
        groupTasks.clear();
    }

    protected void updatePeriodically(@NotNull Group group) {
        ScheduledTask headerFooterTask = null;
        ScheduledTask updateTask = null;
        ScheduledTask latencyTask;

        if (group.headerFooterUpdateRate() > 0) {
            headerFooterTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> updateGroupPlayers(group, false, true))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.headerFooterUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
        }

        if (group.placeholderUpdateRate() > 0) {
            updateTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> updateGroupPlayers(group, true, false))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.placeholderUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
        }

        latencyTask = plugin.getServer().getScheduler()
                .buildTask(plugin, () -> updateLatency(group))
                .delay(1, TimeUnit.SECONDS)
                .repeat(3, TimeUnit.SECONDS)
                .schedule();

        groupTasks.put(group, new GroupTasks(headerFooterTask, updateTask, latencyTask));
    }

    /**
     * Updates the players in the given group.
     *
     * @param group            The group whose players should be updated.
     * @param all              Whether to update all player properties, or just the header and footer.
     * @param incrementIndexes Whether to increment the header and footer indexes.
     */
    private void updateGroupPlayers(@NotNull Group group, boolean all, boolean incrementIndexes) {
        final Set<TabPlayer> groupPlayers = group.getTabPlayers(plugin);
        if (groupPlayers.isEmpty()) {
            return;
        }
        groupPlayers.stream()
                .filter(player -> player.getPlayer().isActive())
                .forEach(player -> {
                    if (incrementIndexes) {
                        player.incrementIndexes();
                    }
                    if (all) {
                        plugin.getTabList().updatePlayer(player, false);
                    }
                    player.sendHeaderAndFooter(plugin.getTabList());
                });
        if (all) {
            plugin.getTabList().updateDisplayNames();
        }
    }

    private void updateLatency(@NotNull Group group) {
        final Set<TabPlayer> groupPlayers = group.getTabPlayers(plugin);
        if (groupPlayers.isEmpty()) {
            return;
        }
        groupPlayers.stream()
                .filter(player -> player.getPlayer().isActive())
                .forEach(player -> {
                    final int latency = (int) player.getPlayer().getPing();
                    final Set<TabPlayer> players = group.getTabPlayers(plugin, player);
                    players.forEach(p -> p.getPlayer().getTabList().getEntry(player.getPlayer().getUniqueId())
                            .ifPresent(entry -> entry.setLatency(Math.max(latency, 0))));
                });
    }

}
