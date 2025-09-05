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
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.types.RelationalAudience;
import net.jodah.expiringmap.ExpiringMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MiniPlaceholdersHook extends Hook {

    private final Map<UUID, TagResolver> cache;

    public MiniPlaceholdersHook(@NotNull Velocitab plugin) {
        super(plugin, "MiniPlaceholders");
        this.cache = ExpiringMap.builder()
                .expiration(5, TimeUnit.MINUTES)
                .build();
    }

    @NotNull
    public Component format(@NotNull String text, @NotNull Player player, @Nullable Player viewer) {
        if (viewer == null) {
            return MiniMessage.miniMessage().deserialize(text, player, MiniPlaceholders.audienceGlobalPlaceholders());
        }

        return MiniMessage.miniMessage().deserialize(text, new RelationalAudience<>(player, viewer), MiniPlaceholders.relationalGlobalPlaceholders());
    }

}
