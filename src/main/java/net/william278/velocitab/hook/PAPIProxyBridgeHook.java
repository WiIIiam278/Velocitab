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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.velocitypowered.api.proxy.Player;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PAPIProxyBridgeHook extends Hook {

    private final PlaceholderAPI api;

    public PAPIProxyBridgeHook(@NotNull Velocitab plugin) {
        super(plugin);
        this.api = PlaceholderAPI.createInstance();
        this.api.setCacheExpiry(Math.max(0, plugin.getSettings().getPapiCacheTime()));
        this.api.setRequestTimeout(1500);
    }

    public CompletableFuture<String> formatPlaceholders(@NotNull String input, @NotNull Player player) {
        return api.formatPlaceholders(input, player.getUniqueId()).thenApply(formattedString -> {
                if (formattedString.contains("ct_")) {
                    return "";
                }
                return formattedString;
            });
    }

    public CompletableFuture<Map<String, String>> parsePlaceholders(@NotNull List<String> input, @NotNull Player player) {
        final Map<String, String> map = Maps.newConcurrentMap();
        final List<CompletableFuture<String>> futures = Lists.newArrayList();

        for (String s : input) {
            final CompletableFuture<String> future = formatPlaceholders(s, player);
            futures.add(future);
            future.thenAccept(r -> map.put(s, r));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(v -> map);
    }

}
