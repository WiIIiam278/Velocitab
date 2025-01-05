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

import io.github.miniplaceholders.api.MiniPlaceholders;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiniPlaceholdersHook extends Hook {

    private final VelocitabMiniExpansion expansion;


    public MiniPlaceholdersHook(@NotNull Velocitab plugin) {
        super(plugin);
        this.expansion = new VelocitabMiniExpansion(plugin);
        expansion.registerExpansion();
    }

    @NotNull
    public Component format(@NotNull String text, @NotNull Audience player, @Nullable Audience viewer) {
        if (viewer == null) {
            return MiniMessage.miniMessage().deserialize(text, MiniPlaceholders.getAudienceGlobalPlaceholders(player));
        }
        return MiniMessage.miniMessage().deserialize(text, MiniPlaceholders.getRelationalGlobalPlaceholders(player, viewer));
    }

    public void unregisterExpansion() {
        expansion.unregisterExpansion();
    }

}
