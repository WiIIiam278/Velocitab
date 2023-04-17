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
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LuckPermsHook extends Hook {

    private int highestWeight = Role.DEFAULT_WEIGHT;
    private final LuckPerms api;

    public LuckPermsHook(@NotNull Velocitab plugin) throws IllegalStateException {
        super(plugin);
        this.api = LuckPermsProvider.get();
        api.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, this::onLuckPermsGroupUpdate);
    }

    @NotNull
    public Role getPlayerRole(@NotNull Player player) {
        return getRoleFromMetadata(getUser(player.getUniqueId()).getCachedData().getMetaData());
    }

    @NotNull
    private Role getRoleFromMetadata(@NotNull CachedMetaData metaData) {
        if (metaData.getPrimaryGroup() == null) {
            return Role.DEFAULT_ROLE;
        }
        return new Role(
                getWeight(metaData.getPrimaryGroup()).orElse(0),
                metaData.getPrimaryGroup(),
                metaData.getPrefix(),
                metaData.getSuffix()
        );
    }

    public void onLuckPermsGroupUpdate(@NotNull UserDataRecalculateEvent event) {
        final PlayerTabList tabList = plugin.getTabList();
        plugin.getServer().getPlayer(event.getUser().getUniqueId())
                .ifPresent(player -> plugin.getServer().getScheduler()
                        .buildTask(plugin, () -> {
                            final TabPlayer updatedPlayer = new TabPlayer(
                                    player,
                                    getRoleFromMetadata(event.getData().getMetaData()),
                                    getHighestWeight()
                            );
                            tabList.replacePlayer(updatedPlayer);
                            tabList.updatePlayer(updatedPlayer);
                        })
                        .delay(500, TimeUnit.MILLISECONDS)
                        .schedule());
    }

    private OptionalInt getWeight(@Nullable String groupName) {
        final Group group;
        if (groupName == null || (group = api.getGroupManager().getGroup(groupName)) == null) {
            return OptionalInt.empty();
        }
        return group.getWeight();
    }

    public int getHighestWeight() {
        if (highestWeight == Role.DEFAULT_WEIGHT) {
            api.getGroupManager().getLoadedGroups().forEach(group -> {
                final OptionalInt weight = group.getWeight();
                if (weight.isPresent() && weight.getAsInt() > highestWeight) {
                    highestWeight = weight.getAsInt();
                }
            });
        }
        return highestWeight;
    }

    private User getUser(@NotNull UUID uuid) {
        return api.getUserManager().getUser(uuid);
    }


}
