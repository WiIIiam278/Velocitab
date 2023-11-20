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

import com.velocitypowered.api.proxy.Player;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PAPIProxyBridgeHook extends Hook {

    private final PlaceholderAPI api;

    public PAPIProxyBridgeHook(@NotNull Velocitab plugin) {
        super(plugin);
        this.api = PlaceholderAPI.createInstance();
        this.api.setCacheExpiry(Math.max(0, plugin.getSettings().getPapiCacheTime()));
    }

    public CompletableFuture<String> formatPlaceholders(@NotNull String input, @NotNull Player player) {
        return api.formatPlaceholders(input, player.getUniqueId());
    }

}
