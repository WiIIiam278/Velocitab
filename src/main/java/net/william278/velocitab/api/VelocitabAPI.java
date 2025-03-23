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
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.PlayerTabList;
import net.william278.velocitab.vanish.VanishIntegration;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Velocitab API.
 * <p>
 * Retrieve an instance of the API class via {@link #getInstance()}.
 *
 * @since 1.5.1
 */
@SuppressWarnings("unused")
public class VelocitabAPI {

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
     * @return instance of the Velocitab API
     * @since 1.5.1
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
     * @hidden This method is for internal use only
     * @since 1.5.1
     */
    @ApiStatus.Internal
    public static void register(@NotNull Velocitab plugin) {
        instance = new VelocitabAPI(plugin);
    }

    /**
     * <b>(Internal use only)</b> - Unregister the API.
     *
     * @hidden This method is for internal use only
     * @since 1.5.1
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
     * @since 1.5.1
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
     * @since 1.5.1
     */
    public void setCustomPlayerName(@NotNull Player player, @Nullable String name) {
        getUser(player).ifPresent(tabPlayer -> {
            tabPlayer.setCustomName(name);
            plugin.getTabList().updateDisplayName(tabPlayer);
        });
    }

    /**
     * Returns the custom name of the TabPlayer, if it has been set.
     *
     * @param player The player for whom to get the custom name
     * @return An Optional object containing the custom name, or empty if no custom name has been set.
     * @since 1.5.1
     */
    public Optional<String> getCustomPlayerName(@NotNull Player player) {
        return getUser(player).flatMap(TabPlayer::getCustomName);
    }

    /**
     * Get the {@link PlayerTabList}, which handles the tab list for players across different server groups.
     *
     * @return the {@link PlayerTabList} global instance.
     * @since 1.5.1
     */
    @NotNull
    public PlayerTabList getTabList() {
        return plugin.getTabList();
    }

    /**
     * Sets the VanishIntegration to use for determining whether the plugin should show a player in the tab list.
     *
     * @param vanishIntegration the VanishIntegration to set
     * @since 1.5.1
     */
    public void setVanishIntegration(@NotNull VanishIntegration vanishIntegration) {
        plugin.getVanishManager().setIntegration(vanishIntegration);
    }

    /**
     * Retrieves the VanishIntegration associated with the VelocitabAPI instance.
     * This integration allows checking if a player can see another player and if a player is vanished.
     *
     * @return The VanishIntegration instance associated with the VelocitabAPI
     * @since 1.5.1
     */
    @NotNull
    public VanishIntegration getVanishIntegration() {
        return plugin.getVanishManager().getIntegration();
    }

    /**
     * Vanishes the player by hiding them from the tab list and scoreboard if enabled.
     *
     * @param player The player to vanish
     * @since 1.5.1
     */
    public void vanishPlayer(@NotNull Player player) {
        plugin.getVanishManager().vanishPlayer(player);
    }

    /**
     * Un-vanishes the given player by showing them in the tab list and scoreboard if enabled.
     *
     * @param player The player to unvanish
     * @since 1.5.1
     */
    public void unVanishPlayer(@NotNull Player player) {
        plugin.getVanishManager().unVanishPlayer(player);
    }

    /**
     * Retrieves the server group that the given player is connected to.
     *
     * @param player the player for whom to retrieve the server group
     * @return the name of the server group that the player is connected to,
     * or a null value if the player is not connected to a server group
     * @since 1.5.1
     */
    @Nullable
    public Group getServerGroup(@NotNull Player player) {
        return getUser(player).map(TabPlayer::getGroup).orElse(null);
    }

    /**
     * Retrieves a list of server groups.
     *
     * @return A list of Group objects representing server groups.
     * @since 1.6.6
     */
    @NotNull
    public List<Group> getServerGroups() {
        return new ArrayList<>(plugin.getTabGroupsManager().getGroups());
    }

    /**
     * Retrieves an optional Group object with the given name.
     *
     * @param name The name of the group to retrieve.
     * @return An optional Group object containing the group with the given name, or an empty optional if no group exists with that name.
     * @since 1.6.6
     */
    @NotNull
    public Optional<Group> getGroup(@NotNull String name) {
        return plugin.getTabGroupsManager().getGroup(name);
    }

    /**
     * Gets a group from the server.
     *
     * @param server The server to get the group from.
     * @return An optional Group object containing the group from the server, or an empty optional if no group exists with that name.
     * @since 1.6.6
     */
    public Optional<Group> getGroupFromServer(@NotNull String server) {
        return plugin.getTabGroupsManager().getGroupFromServer(server, plugin);
    }

    /**
     * An exception indicating the Velocitab API was accessed before it was registered.
     *
     * @since 1.5.1
     */
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
