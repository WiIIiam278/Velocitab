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

package net.william278.velocitab.config;

import com.google.common.collect.Lists;
import com.velocitypowered.api.util.ServerLink;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public record ServerUrl(
        @NotNull String label,
        @NotNull String url,
        @NotNull Set<String> groups
) {

    public ServerUrl(@NotNull String label, @NotNull String url) {
        this(label, url, Set.of("*"));
    }

    // Resolve the built-in label or format the custom label, then wrap as a Velocity ServerLink
    @NotNull
    CompletableFuture<ServerLink> getServerLink(@NotNull Velocitab plugin, @NotNull TabPlayer player) {
        return getBuiltInLabel().map(
                (type) -> CompletableFuture.completedFuture(ServerLink.serverLink(type, url()))
        ).orElseGet(
                () -> Placeholder.replace(label(), plugin, player)
                        .thenApply(replaced -> plugin.getFormatter().format(replaced, player, plugin))
                        .thenApply(formatted -> ServerLink.serverLink(formatted, url()))
        );
    }

    @NotNull
    public static CompletableFuture<List<ServerLink>> resolve(@NotNull Velocitab plugin, @NotNull TabPlayer player,
                                                              @NotNull List<ServerUrl> urls) {
        return resolve(plugin, player, Lists.newArrayList(), Lists.newArrayList(urls));
    }

    // Recursively resolves the ServerLinks for each ServerUrl
    @NotNull
    private static CompletableFuture<List<ServerLink>> resolve(@NotNull Velocitab plugin, @NotNull TabPlayer player,
                                                               @NotNull List<ServerLink> done,
                                                               @NotNull List<ServerUrl> next) {
        if (!next.isEmpty()) {
            return next.get(0).getServerLink(plugin, player).thenCompose(link -> {
                done.add(link);
                return resolve(plugin, player, done, next.subList(1, next.size()));
            });
        }
        return CompletableFuture.completedFuture(done);
    }

    private Optional<ServerLink.Type> getBuiltInLabel() {
        final String label = label().replaceAll(" ", "_").toUpperCase(Locale.ENGLISH);
        return Arrays.stream(ServerLink.Type.values()).filter(type -> type.name().equals(label)).findFirst();
    }


}
