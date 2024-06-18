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

import java.net.URI;
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
        final List<CompletableFuture<ServerLink>> futures = new ArrayList<>();
        for (ServerUrl url : urls) {
            futures.add(url.getServerLink(plugin, player));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join).toList());
    }

    private Optional<ServerLink.Type> getBuiltInLabel() {
        final String label = label().replaceAll(" ", "_").toUpperCase(Locale.ENGLISH);
        return Arrays.stream(ServerLink.Type.values()).filter(type -> type.name().equals(label)).findFirst();
    }

    // Validate a ServerUrl
    void validate() throws IllegalStateException {
        if (label().isEmpty()) {
            throw new IllegalStateException("Server URL label cannot be empty");
        }
        if (url().isEmpty()) {
            throw new IllegalStateException("Server URL cannot be empty");
        }
        if (groups().isEmpty()) {
            throw new IllegalStateException("Server URL must have at least one group, or '*' to show on all groups");
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            URI.create(url());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Server URL is not a valid URI");
        }
    }

}
