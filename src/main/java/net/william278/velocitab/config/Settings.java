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

package net.william278.velocitab.config;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Settings implements ConfigValidator{

    public static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃       Velocitab Config       ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/velocitab
            ┗╸ Documentation: https://william278.net/docs/velocitab""";

    @Comment("Check for updates on startup")
    private boolean checkForUpdates = true;

    @Comment("Whether to remove nametag from players' heads if the nametag associated with their server group is empty.")
    private boolean removeNametags = false;

    @Comment("Which text formatter to use (MINEDOWN, MINIMESSAGE, or LEGACY)")
    private Formatter formatter = Formatter.MINEDOWN;

    @Comment("All servers which are not in other groups will be put in the fallback group."
            + "\n\"false\" will exclude them from Velocitab.")
    private boolean fallbackEnabled = true;

    @Comment("The formats to use for the fallback group.")
    private String fallbackGroup = "default";

    @Comment("Only show other players on a server that is part of the same server group as the player.")
    private boolean onlyListPlayersInSameGroup = true;

    @Comment("Define custom names to be shown in the TAB list for specific server names."
            + "\nIf no custom display name is provided for a server, its original name will be used.")
    private Map<String, String> serverDisplayNames = Map.of("very-long-server-name", "VLSN");

    @Comment("Whether to enable the PAPIProxyBridge hook for PAPI support")
    private boolean enablePapiHook = true;

    @Comment("How long in seconds to cache PAPI placeholders for, in milliseconds. (0 to disable)")
    private long papiCacheTime = 30000;

    @Comment("If you are using MINIMESSAGE formatting, enable this to support MiniPlaceholders in formatting.")
    private boolean enableMiniPlaceholdersHook = true;

    @Comment("Whether to send scoreboard teams packets. Required for player list sorting and nametag formatting."
            + "\nTurn this off if you're using scoreboard teams on backend servers.")
    private boolean sendScoreboardPackets = true;

    @Comment("Whether to sort players in the TAB list.")
    private boolean sortPlayers = true;

    @Comment("Remove gamemode spectator effect for other players in the TAB list.")
    private boolean removeSpectatorEffect = true;

    /**
     * Get display name for the server
     *
     * @param serverName The server name
     * @return The display name, or the server name if no display name is defined
     */
    @NotNull
    public String getServerDisplayName(@NotNull String serverName) {
        return serverDisplayNames.getOrDefault(serverName, serverName);
    }

    @Override
    public void validateConfig(@NotNull Velocitab plugin) {

    }
}
