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
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.util.DebugSystem;
import org.jetbrains.annotations.NotNull;
import org.slf4j.event.Level;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaskManager {

    private final Velocitab plugin;
    private final Map<Group, List<ScheduledFuture<?>>> groupTasks;
    private final ScheduledExecutorService processThread;

    public TaskManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.groupTasks = Maps.newConcurrentMap();
        this.processThread = createProcessThread();
    }

    @NotNull
    private ScheduledExecutorService createProcessThread() {
        final Thread.UncaughtExceptionHandler handler = (t, e) -> plugin.log(Level.ERROR, "Uncaught exception in task manager thread", e);
        return Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread thread = new Thread(r, "Velocitab Task Manager");
            thread.setUncaughtExceptionHandler(handler);
            return thread;
        });
    }

    protected void cancelAllTasks() {
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
                        try {
                            final long startTime = System.currentTimeMillis();
                            plugin.getTabList().updateHeaderFooter(group);
                            final long endTime = System.currentTimeMillis();
                            final long time = endTime - startTime;
                            if (time > 30) {
                                DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Updated header/footer for group {} took {}ms", group.name(), time);
                            }
                        } catch (Throwable e) {
                            plugin.log(Level.ERROR, "Failed to update header/footer for group " + group.name(), e);
                        }
                    },
                    250,
                    Math.max(200, group.headerFooterUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(headerFooterTask);
        }

        if (group.formatUpdateRate() > 0) {
            final ScheduledFuture<?> formatTask = processThread.scheduleAtFixedRate(() -> {
                        try {
                            final long startTime = System.currentTimeMillis();
                            plugin.getTabList().updateGroupNames(group);
                            final long endTime = System.currentTimeMillis();
                            final long time = endTime - startTime;
                            if (time > 50) {
                                DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Updated format for group {} took {}ms", group.name(), time);
                            }
                        } catch (Throwable e) {
                            plugin.log(Level.ERROR, "Failed to update format for group " + group.name(), e);
                        }
                    },
                    500,
                    Math.max(200, group.formatUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(formatTask);
        }

        if (group.nametagUpdateRate() > 0) {
            final ScheduledFuture<?> nametagTask = processThread.scheduleAtFixedRate(() -> {
                        try {
                            final long startTime = System.currentTimeMillis();
                            plugin.getTabList().updateSorting(group);
                            final long endTime = System.currentTimeMillis();
                            final long time = endTime - startTime;
                            if (time > 100) {
                                DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Updated nametags/sorting for group {} took {}ms", group.name(), time);
                            }
                        } catch (Throwable e) {
                            plugin.log(Level.ERROR, "Failed to update nametags/sorting for group " + group.name(), e);
                        }
                    },
                    750,
                    Math.max(200, group.nametagUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(nametagTask);
        }

        if (group.placeholderUpdateRate() > 0) {
            final ScheduledFuture<?> updateTask = processThread.scheduleAtFixedRate(() -> {
                        try {
                            final long startTime = System.currentTimeMillis();
                            updatePlaceholders(group);
                            final long endTime = System.currentTimeMillis();
                            final long time = endTime - startTime;
                            if (time > 10) {
                                DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Updated placeholders for group {} took {}ms", group.name(), time);
                            }
                        } catch (Throwable e) {
                            plugin.log(Level.ERROR, "Failed to update placeholders for group " + group.name(), e);
                        }
                    },
                    1000,
                    Math.max(200, group.placeholderUpdateRate()),
                    TimeUnit.MILLISECONDS);
            tasks.add(updateTask);
        }

        final ScheduledFuture<?> latencyTask = processThread.scheduleAtFixedRate(() -> {
                    try {
                        final long startTime = System.currentTimeMillis();
                        updateLatency(group);
                        final long endTime = System.currentTimeMillis();
                        final long time = endTime - startTime;
                        if (time > 25) {
                            DebugSystem.log(DebugSystem.DebugLevel.DEBUG, "Updated latency for group {} took {}ms", group.name(), time);
                        }
                    } catch (Throwable e) {
                        plugin.log(Level.ERROR, "Failed to update latency for group " + group.name(), e);
                    }
                },
                1250,
                5000,
                TimeUnit.MILLISECONDS);

        tasks.add(latencyTask);
    }

    private void updatePlaceholders(@NotNull Group group) {
        final List<TabPlayer> players = group.getTabPlayers(plugin);
        if (players.isEmpty()) {
            return;
        }

        final List<String> texts = group.getTextsWithPlaceholders(plugin);
        players.forEach(player -> plugin.getPlaceholderManager().fetchPlaceholders(player.getPlayer().getUniqueId(), texts, group));
    }

    private void updateLatency(@NotNull Group group) {
        final List<TabPlayer> groupPlayers = group.getTabPlayers(plugin);
        if (groupPlayers.isEmpty()) {
            return;
        }

        groupPlayers.forEach(player -> {
            final int latency = (int) player.getPlayer().getPing();
            groupPlayers.forEach(p -> p.getPlayer().getTabList().getEntry(player.getPlayer().getUniqueId())
                    .ifPresent(entry -> entry.setLatency(Math.max(latency, 0))));
        });
    }

    public void run(@NotNull Runnable runnable) {
        try {
            processThread.execute(runnable);
        } catch (Throwable e) {
            plugin.log(Level.ERROR, "Failed to run task", e);
        }
    }

    public void runDelayed(@NotNull Runnable runnable, long delay, @NotNull TimeUnit timeUnit) {
        try {
            processThread.schedule(runnable, delay, timeUnit);
        } catch (Throwable e) {
            plugin.log(Level.ERROR, "Failed to run delayed task", e);
        }
    }
}
