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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaskManager {

    private final Velocitab plugin;
    private final Map<Group, List<ScheduledFuture<?>>> groupTasks;
    private final Map<Group, List<ScheduledTask>> groupTasksOld;
    private final ScheduledExecutorService processThread;

    public TaskManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.groupTasksOld = Maps.newConcurrentMap();
        this.groupTasks = Maps.newConcurrentMap();
        this.processThread = Executors.newSingleThreadScheduledExecutor();
    }

    protected void cancelAllTasks() {
        groupTasksOld.values().forEach(c -> c.forEach(ScheduledTask::cancel));
        groupTasksOld.clear();
        groupTasks.values().forEach(c -> c.forEach(t -> t.cancel(true)));
        groupTasks.clear();
    }

    public void close() {
        try {
            cancelAllTasks();
            processThread.shutdownNow();
        } catch (Throwable e) {
            plugin.getLogger().error("Failed to close task manager", e);
        }
    }

    protected void updatePeriodically(@NotNull Group group) {
        final List<ScheduledFuture<?>> tasks = groupTasks.computeIfAbsent(group, g -> Lists.newArrayList());
        if (group.headerFooterUpdateRate() > 0) {
            final ScheduledFuture<?> headerFooterTask = processThread.scheduleAtFixedRate(() -> {
                        final long startTime = System.currentTimeMillis();
                        plugin.getTabList().updateHeaderFooter(group);
                        final long endTime = System.currentTimeMillis();
                        final long time = endTime - startTime;
                        if (plugin.getSettings().isDebug()) {
                            plugin.getLogger().info("Updated header/footer for group {} took {}ms", group.name(), time);
                        }
                    },
                    250,
                    Math.max(200, group.headerFooterUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(headerFooterTask);
        }

        if (group.formatUpdateRate() > 0) {
            final ScheduledFuture<?> formatTask = processThread.scheduleAtFixedRate(() -> {
                        final long startTime = System.currentTimeMillis();
                        plugin.getTabList().updateGroupNames(group);
                        final long endTime = System.currentTimeMillis();
                        final long time = endTime - startTime;
                        if (plugin.getSettings().isDebug()) {
                            plugin.getLogger().info("Updated format for group {} took {}ms", group.name(), time);
                        }
                    },
                    500,
                    Math.max(200, group.formatUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(formatTask);
        }

        if (group.nametagUpdateRate() > 0) {
            final ScheduledFuture<?> nametagTask = processThread.scheduleAtFixedRate(() -> {
                        final long startTime = System.currentTimeMillis();
                        plugin.getTabList().updateSorting(group);
                        final long endTime = System.currentTimeMillis();
                        final long time = endTime - startTime;
                        if (plugin.getSettings().isDebug()) {
                            plugin.getLogger().info("Updated nametags/sorting for group {} took {}ms", group.name(), time);
                        }
                    },
                    750,
                    Math.max(200, group.nametagUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(nametagTask);
        }

        if (group.placeholderUpdateRate() > 0) {
            final ScheduledFuture<?> updateTask = processThread.scheduleAtFixedRate(() -> {
                        final long startTime = System.currentTimeMillis();
                        updatePlaceholders(group);
                        final long endTime = System.currentTimeMillis();
                        final long time = endTime - startTime;
                        if (plugin.getSettings().isDebug()) {
                            plugin.getLogger().info("Updated placeholders for group {} took {}ms", group.name(), time);
                        }
                    },
                    1000,
                    Math.max(200, group.placeholderUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(updateTask);
        }

        final ScheduledFuture<?> latencyTask = processThread.scheduleAtFixedRate(() -> {
                    final long startTime = System.currentTimeMillis();
                    updateLatency(group);
                    final long endTime = System.currentTimeMillis();
                    final long time = endTime - startTime;
                    if (plugin.getSettings().isDebug()) {
                        plugin.getLogger().debug("Updated latency for group {} took {}ms", group.name(), time);
                    }
                },
                1250,
                2500,
                TimeUnit.MILLISECONDS);

        tasks.add(latencyTask);
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

    public void run(@NotNull Runnable runnable) {
        processThread.execute(runnable);
    }

    public void runDelayed(@NotNull Runnable runnable, long delay, @NotNull TimeUnit timeUnit) {
        processThread.schedule(runnable, delay, timeUnit);
    }
}
