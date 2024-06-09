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

package net.william278.velocitab.providers;

import net.william278.velocitab.Velocitab;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.sorting.SortingManager;
import net.william278.velocitab.tab.PlayerTabList;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface ScoreboardProvider {

    /**
     * Retrieves the Velocitab plugin instance.
     *
     * @return The Velocitab plugin instance.
     */
    Velocitab getPlugin();

    /**
     * Retrieves the optional scoreboard manager.
     *
     * @return An {@code Optional} object that may contain a {@code ScoreboardManager} instance.
     */
    Optional<ScoreboardManager> getScoreboardManager();

    /**
     * Sets the scoreboard manager.
     *
     * @param scoreboardManager The scoreboard manager to be set.
     */
    void setScoreboardManager(ScoreboardManager scoreboardManager);

    /**
     * Retrieves the tab list for the player.
     *
     * @return The PlayerTabList object representing the tab list for the player.
     */
    PlayerTabList getTabList();

    /**
     * Sets the tab list for the player.
     *
     * @param tabList The PlayerTabList object representing the tab list to be set for the player.
     */
    void setTabList(PlayerTabList tabList);

    /**
     * Returns the SortingManager instance.
     *
     * @return The SortingManager instance.
     */
    SortingManager getSortingManager();

    /**
     * Sets the sorting manager for the ScoreboardProvider.
     *
     * @param sortingManager The sorting manager to be set.
     */
    void setSortingManager(SortingManager sortingManager);

    /**
     * Prepares the scoreboard by initializing the necessary components.
     * This method is responsible for setting up the scoreboard manager, player tab list,
     * scheduler tasks, and sorting manager.
     *
     */
    default void prepareScoreboard() {
        if (getPlugin().getSettings().isSendScoreboardPackets()) {
            final ScoreboardManager scoreboardManager = new ScoreboardManager(getPlugin());
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

    /**
     * Disables the ScoreboardManager and closes the tab list for the player.
     */
    default void disableScoreboardManager() {
        if (getScoreboardManager().isPresent() && getPlugin().getSettings().isSendScoreboardPackets()) {
            getScoreboardManager().get().close();
            getScoreboardManager().get().unregisterPacket();
        }

        getTabList().close();
    }

}
