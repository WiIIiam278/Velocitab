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
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Utility class responsible for firing Velocitab API events with timeout protection
 * and snapshot-based rollback to prevent partial mutations from misbehaving listeners.
 *
 * <p>If a listener does not complete within {@link #EVENT_TIMEOUT_MS} milliseconds, the
 * original (pre-event) component values are used and a warning is logged.</p>
 *
 * @since 1.6.9
 */
@RequiredArgsConstructor
public class EventDispatcher {

    /**
     * Maximum time in milliseconds a set of event listeners may collectively take.
     * If exceeded, original values are used and a warning is logged.
     */
    public static final long EVENT_TIMEOUT_MS = 50;

    @NotNull
    private final Velocitab plugin;

    /**
     * Fires a {@link TabDisplayNameEvent} and returns the (possibly modified) display name.
     *
     * <p>Original values are snapshotted before firing. On timeout or exception the
     * snapshot is returned, guaranteeing no partial mutation reaches the caller.</p>
     *
     * @param player      the player whose display name is being set
     * @param viewer      the player who will see the entry
     * @param displayName the pre-computed display name (snapshot)
     * @return the display name to use, possibly modified by a listener
     */
    @NotNull
    public Component fireDisplayNameEvent(@NotNull TabPlayer player, @NotNull TabPlayer viewer,
                                          @NotNull Component displayName) {
        final TabDisplayNameEvent event = new TabDisplayNameEvent(player, viewer, displayName);
        try {
            plugin.getServer().getEventManager()
                    .fire(event)
                    .get(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return event.getDisplayName();
        } catch (TimeoutException e) {
            plugin.log(Level.WARN, "TabDisplayNameEvent timed out after %dms, using original value"
                    .formatted(EVENT_TIMEOUT_MS));
            return displayName;
        } catch (Exception e) {
            plugin.log(Level.ERROR, "Error firing TabDisplayNameEvent", e);
            return displayName;
        }
    }

    /**
     * Fires a {@link TabHeaderFooterEvent} and returns the (possibly modified) header and footer
     * as a two-element array {@code [header, footer]}.
     *
     * <p>Original values are snapshotted before firing. On timeout or exception the
     * snapshots are returned, guaranteeing no partial mutation reaches the caller.</p>
     *
     * @param player the player who will receive the header/footer
     * @param header the pre-computed header component (snapshot)
     * @param footer the pre-computed footer component (snapshot)
     * @return a two-element array containing {@code [header, footer]} to use
     */
    @NotNull
    public Component[] fireHeaderFooterEvent(@NotNull TabPlayer player,
                                             @NotNull Component header,
                                             @NotNull Component footer) {
        final TabHeaderFooterEvent event = new TabHeaderFooterEvent(player, header, footer);
        try {
            plugin.getServer().getEventManager()
                    .fire(event)
                    .get(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return new Component[]{event.getHeader(), event.getFooter()};
        } catch (TimeoutException e) {
            plugin.log(Level.WARN, "TabHeaderFooterEvent timed out after %dms, using original values"
                    .formatted(EVENT_TIMEOUT_MS));
            return new Component[]{header, footer};
        } catch (Exception e) {
            plugin.log(Level.ERROR, "Error firing TabHeaderFooterEvent", e);
            return new Component[]{header, footer};
        }
    }

    /**
     * Fires a {@link TabTeamUpdateEvent} and returns the (possibly modified) event.
     *
     * <p>Original values are snapshotted before firing. On timeout or exception the
     * snapshots are returned via a restored event, guaranteeing no partial mutation
     * reaches the caller.</p>
     *
     * @param player      the player whose nametag is being sent
     * @param viewer      the player receiving the team packet
     * @param prefix      the pre-computed prefix component (snapshot), or null
     * @param suffix      the pre-computed suffix component (snapshot), or null
     * @param displayName the pre-computed display name component (snapshot), or null
     * @param mode        whether this is a CREATE or UPDATE packet
     * @return the event whose fields reflect the final component values to use
     */
    @NotNull
    public TabTeamUpdateEvent fireTeamUpdateEvent(@NotNull TabPlayer player, @NotNull Player viewer,
                                                  @Nullable Component prefix, @Nullable Component suffix,
                                                  @Nullable Component displayName,
                                                  @NotNull TabTeamUpdateEvent.Mode mode) {
        final TabTeamUpdateEvent event = new TabTeamUpdateEvent(player, viewer, prefix, suffix, displayName, mode);
        try {
            plugin.getServer().getEventManager()
                    .fire(event)
                    .get(EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return event;
        } catch (TimeoutException e) {
            plugin.log(Level.WARN, "TabTeamUpdateEvent timed out after %dms, using original values"
                    .formatted(EVENT_TIMEOUT_MS));
            // Restore snapshots to guard against partial mutation
            return new TabTeamUpdateEvent(player, viewer, prefix, suffix, displayName, mode);
        } catch (Exception e) {
            plugin.log(Level.ERROR, "Error firing TabTeamUpdateEvent", e);
            return new TabTeamUpdateEvent(player, viewer, prefix, suffix, displayName, mode);
        }
    }

}
