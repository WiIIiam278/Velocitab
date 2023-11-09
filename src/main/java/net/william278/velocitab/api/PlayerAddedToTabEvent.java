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

package net.william278.velocitab.api;

import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("unused")
public class PlayerAddedToTabEvent {

    private final TabPlayer player;
    private final String group;
    private final List<String> groupServers;

    public PlayerAddedToTabEvent(@NotNull TabPlayer player, @NotNull String group, @NotNull List<String> groupServers) {
        this.player = player;
        this.group = group;
        this.groupServers = groupServers;
    }

    @NotNull
    public TabPlayer getTabPlayer() {
        return this.player;
    }

    @NotNull
    public String getGroup() {
        return this.group;
    }

    @NotNull
    public List<String> getGroupServers() {
        return this.groupServers;
    }

    @NotNull
    @Deprecated(forRemoval = true)
    private TabPlayer player() {
        return this.player;
    }

    @NotNull
    @Deprecated(forRemoval = true)
    private String group() {
        return this.group;
    }

    @NotNull
    @Deprecated(forRemoval = true)
    private List<String> groupServers() {
        return this.groupServers;
    }
}
