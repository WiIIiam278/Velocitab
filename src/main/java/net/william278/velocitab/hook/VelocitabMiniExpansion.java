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
import io.github.miniplaceholders.api.Expansion;
import io.github.miniplaceholders.api.MiniPlaceholders;
import io.github.miniplaceholders.api.utils.TagsUtils;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;

import java.time.LocalTime;
import java.util.Optional;

@RequiredArgsConstructor
public class VelocitabMiniExpansion {

    private final Velocitab plugin;
    private Expansion expansion;

    public void registerExpansion() {
        final Expansion.Builder builder = Expansion.builder("velocitab");
        builder.relationalPlaceholder("who-is-seeing", ((audience, otherAudience, queue, ctx) -> {
            if (!(otherAudience instanceof Player target)) {
                return TagsUtils.EMPTY_TAG;
            }
            if (!(audience instanceof Player)) {
                return TagsUtils.EMPTY_TAG;
            }

            return Tag.selfClosingInserting(Component.text(target.getUsername() + "-" + LocalTime.now().getSecond()));
        }));
        builder.relationalPlaceholder("perm", ((audience, otherAudience, queue, ctx) -> {
            if (!(otherAudience instanceof Player target)) {
                return TagsUtils.EMPTY_TAG;
            }
            if (!(audience instanceof Player player)) {
                return TagsUtils.EMPTY_TAG;
            }

            final Optional<TabPlayer> targetOptional = plugin.getTabList().getTabPlayer(player);
            if (targetOptional.isEmpty()) {
                return TagsUtils.EMPTY_TAG;
            }

            final TabPlayer targetPlayer = targetOptional.get();

            if(!queue.hasNext()) {
                return TagsUtils.EMPTY_TAG;
            }

            final String permission = queue.pop().value();

            if(!queue.hasNext()) {
                return TagsUtils.EMPTY_TAG;
            }

            if(!target.hasPermission(permission)) {
                return TagsUtils.EMPTY_TAG;
            }

            final String value = queue.pop().value();
            final String replaced = Placeholder.replaceInternal(value, plugin, targetPlayer);
            return Tag.selfClosingInserting(MiniMessage.miniMessage().deserialize(replaced, MiniPlaceholders.getAudienceGlobalPlaceholders(player)));
        }));
        builder.relationalPlaceholder("vanish", ((audience, otherAudience, queue, ctx) -> {
            if (!(otherAudience instanceof Player target)) {
                return TagsUtils.EMPTY_TAG;
            }
            if (!(audience instanceof Player player)) {
                return TagsUtils.EMPTY_TAG;
            }

            return Tag.selfClosingInserting(Component.text(plugin.getVanishManager().getIntegration().canSee(player.getUsername(), target.getUsername())));
        }));
        plugin.getLogger().info("Registered Velocitab MiniExpansion");
        expansion = builder.build();
        expansion.register();
    }

    public void unregisterExpansion() {
        expansion.unregister();
    }
}
