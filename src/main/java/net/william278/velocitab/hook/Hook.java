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

package net.william278.velocitab.hook;

import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class Hook {

    public static final List<Function<Velocitab, Optional<Hook>>> AVAILABLE = List.of(
            (plugin -> {
                if (isPluginAvailable(plugin, "luckperms")) {
                    try {
                        plugin.log("Successfully hooked into LuckPerms");
                        return Optional.of(new LuckPermsHook(plugin));
                    } catch (Exception e) {
                        plugin.log("LuckPerms hook was not loaded: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            }),
            (plugin -> {
                if (isPluginAvailable(plugin, "papiproxybridge") && plugin.getSettings().isEnablePapiHook()) {
                    try {
                        plugin.log("Successfully hooked into PAPIProxyBridge");
                        return Optional.of(new PapiHook(plugin));
                    } catch (Exception e) {
                        plugin.log("PAPIProxyBridge hook was not loaded: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            }),
            (plugin -> {
                if (isPluginAvailable(plugin, "miniplaceholders") && plugin.getSettings().isEnableMiniPlaceholdersHook()) {
                    try {
                        plugin.log("Successfully hooked into MiniPlaceholders");
                        return Optional.of(new MiniPlaceholdersHook(plugin));
                    } catch (Exception e) {
                        plugin.log("MiniPlaceholders hook was not loaded: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            })
    );

    protected final Velocitab plugin;

    public Hook(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    private static boolean isPluginAvailable(@NotNull Velocitab plugin, @NotNull String id) {
        return plugin.getServer().getPluginManager().getPlugin(id).isPresent();
    }

}
