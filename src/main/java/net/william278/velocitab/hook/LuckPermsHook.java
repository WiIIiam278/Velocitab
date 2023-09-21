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
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class LuckPermsHook extends Hook {

    private int highestWeight = Role.DEFAULT_WEIGHT;
    private final LuckPerms api;
    private final EventSubscription<UserDataRecalculateEvent> event;
    private final Map<UUID, Long> lastUpdate;

    public LuckPermsHook(@NotNull Velocitab plugin) throws IllegalStateException {
        super(plugin);
        this.api = LuckPermsProvider.get();
        lastUpdate = new HashMap<>();
        event = api.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, this::onLuckPermsGroupUpdate);

    }

    public void close() {
        event.close();
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
        final Optional<Group> group = getGroup(metaData.getPrimaryGroup());
        return new Role(
                group.map(this::getGroupWeight).orElse(Role.DEFAULT_WEIGHT),
                metaData.getPrimaryGroup(),
                group.map(Group::getDisplayName).orElse(metaData.getPrimaryGroup()),
                metaData.getPrefix(),
                metaData.getSuffix()
        );
    }

    public void onLuckPermsGroupUpdate(@NotNull UserDataRecalculateEvent event) {
        if (!plugin.isActive()) return;
        if (lastUpdate.getOrDefault(event.getUser().getUniqueId(), 0L) > System.currentTimeMillis() - 100) return;
        lastUpdate.put(event.getUser().getUniqueId(), System.currentTimeMillis());
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
                            tabList.updatePlayerDisplayName(updatedPlayer);
                        })
                        .delay(500, TimeUnit.MILLISECONDS)
                        .schedule());
    }

    // Get a group by name
    private Optional<Group> getGroup(@Nullable String groupName) {
        if (groupName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(api.getGroupManager().getGroup(groupName));
    }

    // Get the weight of a group
    private int getGroupWeight(@NotNull Group group) {
        return group.getWeight().orElse(Role.DEFAULT_WEIGHT);
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
