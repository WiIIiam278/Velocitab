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

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;

public interface LoggerProvider {

    Logger getLogger();

    default void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... exceptions) {
        switch (level) {
            case ERROR -> {
                if (exceptions.length > 0) {
                    getLogger().error(message, exceptions[0]);
                } else {
                    getLogger().error(message);
                }
            }
            case WARN -> {
                if (exceptions.length > 0) {
                    getLogger().warn(message, exceptions[0]);
                } else {
                    getLogger().warn(message);
                }
            }
            case INFO -> getLogger().info(message);
        }
    }

    default void log(@NotNull String message) {
        this.log(Level.INFO, message);
    }

}
