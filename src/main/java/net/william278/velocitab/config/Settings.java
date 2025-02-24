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

import java.util.List;


@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Settings implements ConfigValidator {

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

    @Comment("Whether to disable header and footer if they are empty and let backend servers handle them.")
    private boolean disableHeaderFooterIfEmpty = true;

    @Comment("Which text formatter to use (MINIMESSAGE, MINEDOWN or LEGACY)")
    private Formatter formatter = Formatter.MINIMESSAGE;

    @Comment("All servers which are not in other groups will be put in the fallback group."
            + "\n\"false\" will exclude them from Velocitab.")
    private boolean fallbackEnabled = true;

    @Comment("The formats to use for the fallback group.")
    private String fallbackGroup = "default";

    @Comment("Whether to show all players from all groups in the TAB list.")
    private boolean showAllPlayersFromAllGroups = false;

    @Comment("Whether to enable the PAPIProxyBridge hook for PAPI support")
    private boolean enablePapiHook = true;

    @Comment("How long in seconds to cache PAPI placeholders for, in milliseconds. (0 to disable)")
    private long papiCacheTime = 30000;

    @Comment("If you are using MINIMESSAGE formatting, enable this to support MiniPlaceholders in formatting.")
    private boolean enableMiniPlaceholdersHook = true;

    @Comment("Whether to send scoreboard teams packets. Required for player list sorting and nametag formatting."
            + "\nTurn this off if you're using scoreboard teams on backend servers.")
    private boolean sendScoreboardPackets = true;

    @Comment("If built-in placeholders return a blank string, fallback to Placeholder API equivalents.\n"
            + "For example, if %prefix% returns a blank string, use %luckperms_prefix%. Requires PAPIProxyBridge.")
    private boolean fallbackToPapiIfPlaceholderBlank = false;

    @Comment("Whether to sort players in the TAB list.")
    private boolean sortPlayers = true;

    @Comment("Remove gamemode spectator effect for other players in the TAB list.")
    private boolean removeSpectatorEffect = true;

    @Comment("Whether to enable the Plugin Message API (allows backend plugins to perform certain operations)")
    private boolean enablePluginMessageApi = true;

    @Comment("Whether to force sending tab list packets to all players, even if a packet for that action has already been sent. This could fix issues with some mods.")
    private boolean forceSendingTabListPackets = false;

    @Comment("Whether to enable relational placeholders. With an high amount of players, this could cause lag.")
    private boolean enableRelationalPlaceholders = false;

    @Comment({"A list of links that will be sent to display on player pause menus (Minecraft 1.21+ clients only).",
            "• Labels can be fully custom or built-in (one of 'bug_report', 'community_guidelines', 'support', 'status',",
            "  'feedback', 'community', 'website', 'forums', 'news', or 'announcements').",
            "• If you supply a url with a 'bug_report' label, it will be shown if the player is disconnected.",
            "• Specify a set of server groups each URL should be sent on. Use '*' to show a URL to all groups."})
    private List<ServerUrl> serverLinks = List.of(
            new ServerUrl(
                    "<#00fb9a>About Velocitab</#00fb9a>",
                    "https://william278.net/project/velocitab"
            )
    );

    @NotNull
    public List<ServerUrl> getUrlsForGroup(@NotNull Group group) {
        return serverLinks.stream()
                .filter(link -> link.groups().contains("*") || link.groups().contains(group.name()))
                .toList();
    }

    @Override
    public void validateConfig(@NotNull Velocitab plugin, @NotNull String name) {
        if (papiCacheTime < 0) {
            throw new IllegalStateException("PAPI cache time must be greater than or equal to 0");
        }
        serverLinks.forEach(ServerUrl::validate);
    }

}
