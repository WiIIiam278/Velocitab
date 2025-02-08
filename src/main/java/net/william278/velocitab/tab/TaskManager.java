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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TaskManager {

    private final Velocitab plugin;
    private final Map<Group, List<ScheduledTask>> groupTasks;

    public TaskManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.groupTasks = Maps.newConcurrentMap();
    }

    protected void cancelAllTasks() {
        groupTasks.values().forEach(c -> c.forEach(ScheduledTask::cancel));
        groupTasks.clear();
    }

    protected void updatePeriodically(@NotNull Group group) {
        if (group.headerFooterUpdateRate() > 0) {
            final ScheduledTask headerFooterTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> plugin.getTabList().updateHeaderFooter(group))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.headerFooterUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            groupTasks.computeIfAbsent(group, g -> Lists.newArrayList()).add(headerFooterTask);
        }

        if (group.formatUpdateRate() > 0) {
            final ScheduledTask formatTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> plugin.getTabList().updateDisplayNames(group))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.formatUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            groupTasks.computeIfAbsent(group, g -> Lists.newArrayList()).add(formatTask);
        }

        if (group.nametagUpdateRate() > 0) {
            final ScheduledTask nametagTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> plugin.getTabList().updateSorting(group))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.nametagUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            groupTasks.computeIfAbsent(group, g -> Lists.newArrayList()).add(nametagTask);
        }

        if (group.placeholderUpdateRate() > 0) {
            final ScheduledTask updateTask = plugin.getServer().getScheduler()
                    .buildTask(plugin, () -> updatePlaceholders(group))
                    .delay(1, TimeUnit.SECONDS)
                    .repeat(Math.max(200, group.placeholderUpdateRate()), TimeUnit.MILLISECONDS)
                    .schedule();
            groupTasks.computeIfAbsent(group, g -> Lists.newArrayList()).add(updateTask);
        }

        final ScheduledTask latencyTask = plugin.getServer().getScheduler()
                .buildTask(plugin, () -> updateLatency(group))
                .delay(1, TimeUnit.SECONDS)
                .repeat(3, TimeUnit.SECONDS)
                .schedule();

        groupTasks.computeIfAbsent(group, g -> Lists.newArrayList()).add(latencyTask);
    }

    private void updatePlaceholders(@NotNull Group group) {
        final List<TabPlayer> players = group.getTabPlayersAsList(plugin);
        if (players.isEmpty()) {
            return;
        }

        final List<String> texts = group.getTextsWithPlaceholders();
        players.forEach(player -> plugin.getPlaceholderManager().fetchPlaceholders(player.getPlayer().getUniqueId(), texts, group));
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
