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

package net.william278.velocitab.util;

import net.william278.velocitab.Velocitab;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.sorting.SortingManager;
import net.william278.velocitab.tab.PlayerTabList;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface ScoreboardProvider {

    Velocitab getPlugin();

    Optional<ScoreboardManager> getScoreboardManager();

    void setScoreboardManager(ScoreboardManager scoreboardManager);

    PlayerTabList getTabList();

    void setTabList(PlayerTabList tabList);

    SortingManager getSortingManager();

    void setSortingManager(SortingManager sortingManager);

    default void prepareScoreboard() {
        if (getPlugin().getSettings().isSendScoreboardPackets()) {
            ScoreboardManager scoreboardManager = new ScoreboardManager(getPlugin());
            setScoreboardManager(scoreboardManager);
            scoreboardManager.registerPacket();
        }

        final PlayerTabList tabList = new PlayerTabList(getPlugin());
        setTabList(tabList);
        getPlugin().getServer().getEventManager().register(this, tabList);

        getPlugin().getServer().getScheduler().buildTask(this, tabList::load).delay(1, TimeUnit.SECONDS).schedule();

        final SortingManager sortingManager = new SortingManager(getPlugin());
        setSortingManager(sortingManager);
    }

    default void disableScoreboardManager() {
        if (getScoreboardManager().isPresent() && getPlugin().getSettings().isSendScoreboardPackets()) {
            getScoreboardManager().get().close();
            getScoreboardManager().get().unregisterPacket();
        }

        getTabList().close();
    }

}
