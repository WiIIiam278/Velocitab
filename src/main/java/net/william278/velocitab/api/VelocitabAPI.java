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

import com.velocitypowered.api.proxy.Player;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.PlayerTabList;
import net.william278.velocitab.vanish.VanishIntegration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("unused")

/**
 * The Velocitab API class.
 * Retrieve an instance of the API class via {@link #getInstance()}.
 */ public class VelocitabAPI {

    // Instance of the plugin
    private final Velocitab plugin;
    private static VelocitabAPI instance;

    @ApiStatus.Internal
    protected VelocitabAPI(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    /**
     * Entrypoint to the {@link VelocitabAPI} API - returns an instance of the API
     *
     * @return instance of the HuskSync API
     * @since 1.5.2
     */
    @NotNull
    public static VelocitabAPI getInstance() {
        if (instance == null) {
            throw new NotRegisteredException();
        }
        return instance;
    }

    /**
     * <b>(Internal use only)</b> - Register the API.
     *
     * @param plugin the plugin instance
     * @since 3.0
     */
    @ApiStatus.Internal
    public static void register(@NotNull Velocitab plugin) {
        instance = new VelocitabAPI(plugin);
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API.
     */
    @ApiStatus.Internal
    public static void unregister() {
        instance = null;
    }

    /**
     * Returns an option of {@link TabPlayer} instance for the given Velocity {@link Player}.
     *
     * @param player the Velocity player to get the {@link TabPlayer} instance for
     * @return the {@link TabPlayer} instance for the given player or an empty optional if the player is not in a group server
     * @since 2.0
     */
    public Optional<TabPlayer> getUser(@NotNull Player player) {
        return plugin.getTabList().getTabPlayer(player);
    }

    /**
     * Sets the custom name for a player.
     * This will only be visible in the tab list and not in the nametag.
     *
     * @param player The player for whom to set the custom name
     * @param name   The custom name to set
     */
    public void setCustomPlayerName(@NotNull Player player, @Nullable String name) {
        getUser(player).ifPresent(tabPlayer -> {
            tabPlayer.setCustomName(name);
            plugin.getTabList().updatePlayerDisplayName(tabPlayer);
        });
    }

    /**
     * Returns the custom name of the TabPlayer, if it has been set.
     *
     * @param player The player for whom to get the custom name
     * @return An Optional object containing the custom name, or empty if no custom name has been set.
     */
    public Optional<String> getCustomPlayerName(@NotNull Player player) {
        return getUser(player).flatMap(TabPlayer::getCustomName);
    }

    /**
     * {@link PlayerTabList} handles the tab list for all players on the server groups.
     *
     * @return the {@link PlayerTabList} global instance.
     */
    @NotNull
    public PlayerTabList getTabList() {
        return plugin.getTabList();
    }

    /**
     * Sets the VanishIntegration for the VelocitabAPI.
     *
     * @param vanishIntegration the VanishIntegration to set
     */
    public void setVanishIntegration(@NotNull VanishIntegration vanishIntegration) {
        plugin.getVanishManager().setIntegration(vanishIntegration);
    }

    /**
     * Retrieves the VanishIntegration associated with the VelocitabAPI instance.
     * This integration allows checking if a player can see another player and if a player is vanished.
     *
     * @return The VanishIntegration instance associated with the VelocitabAPI
     */
    @NotNull
    public VanishIntegration getVanishIntegration() {
        return plugin.getVanishManager().getIntegration();
    }

    /**
     * Vanishes the player by hiding them from the tab list and scoreboard if enabled.
     *
     * @param player The player to vanish
     */
    public void vanishPlayer(@NotNull Player player) {
        plugin.getVanishManager().vanishPlayer(player);
    }

    /**
     * Un-vanishes the given player by showing them in the tab list and scoreboard if enabled.
     *
     * @param player The player to unvanish
     */
    public void unVanishPlayer(@NotNull Player player) {
        plugin.getVanishManager().unVanishPlayer(player);
    }


    static final class NotRegisteredException extends IllegalStateException {

        private static final String MESSAGE = """
                Could not access the Velocitab API as it has not yet been registered. This could be because:
                1) Velocitab has failed to enable successfully
                2) You are attempting to access Velocitab on plugin construction/before your plugin has enabled.
                3) You have shaded Velocitab into your plugin jar and need to fix your maven/gradle/build script
                   to only include Velocitab as a dependency and not as a shaded dependency.""";

        NotRegisteredException() {
            super(MESSAGE);
        }

    }
}
