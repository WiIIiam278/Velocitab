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

package net.william278.velocitab.util;

import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Iterator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DebugSystem {

    private static final ConcurrentLinkedQueue<LogEntry> logs = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOGS = 10000;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int REMOVE_HOURS = 6;

    public enum DebugLevel {
        INFO, WARNING, ERROR, DEBUG
    }

    private static class LogEntry {
        final long timestamp;
        final String threadName;
        final DebugLevel level;
        final String message;

        LogEntry(@NotNull final String threadName, @NotNull final DebugLevel level, @NotNull final String message) {
            this.timestamp = System.currentTimeMillis();
            this.threadName = threadName;
            this.level = level;
            this.message = message;
        }

        @NotNull
        public String format() {
            return "[" + DATE_FORMAT.format(new Date(timestamp)) + "] [" + threadName + "/" + level + "]: " + message;
        }
    }

    public static void log(@NotNull final DebugLevel level, @NotNull final String message) {
        logs.add(new LogEntry(Thread.currentThread().getName(), level, message));
        if (logs.size() > MAX_LOGS) {
            logs.poll();
        }
    }

    public static void log(@NotNull DebugLevel level, @NotNull String message, Object... args) {
        logs.add(new LogEntry(Thread.currentThread().getName(), level, formatMessage(message, args)));
        if (logs.size() > MAX_LOGS) {
            logs.poll();
        }
    }

    @NotNull
    private static String formatMessage(@NotNull final String message, @NotNull final Object... args) {
        final StringBuilder formattedMessage = new StringBuilder();
        int argIndex = 0;
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '{' && i + 1 < message.length() && message.charAt(i + 1) == '}') {
                formattedMessage.append(argIndex < args.length ? args[argIndex++] : "{}");
                i++;
            } else {
                formattedMessage.append(message.charAt(i));
            }
        }

        return formattedMessage.toString();
    }

    @NotNull
    public static String getLogsAsString() {
        final StringBuilder logBuilder = new StringBuilder();
        for (final LogEntry entry : logs) {
            logBuilder.append(entry.format()).append("\n");
        }
        return logBuilder.toString();
    }

    private static void removeLogsOlderThan() {
        final long cutoffTime = System.currentTimeMillis() - (DebugSystem.REMOVE_HOURS * 3600L * 1000L);
        final Iterator<LogEntry> iterator = logs.iterator();

        while (iterator.hasNext()) {
            final LogEntry entry = iterator.next();
            if (entry.timestamp < cutoffTime) {
                iterator.remove();
            } else {
                break;
            }
        }
    }

    public static void initializeTask(@NotNull Velocitab plugin) {
        plugin.getServer().getScheduler().buildTask(plugin, DebugSystem::removeLogsOlderThan)
                .delay(REMOVE_HOURS, TimeUnit.HOURS)
                .repeat(REMOVE_HOURS, TimeUnit.HOURS)
                .schedule();
    }
}

