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

import lombok.Getter;
import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@YamlFile(header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃       Velocitab Config       ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┣╸ Information: https://william278.net/project/velocitab
        ┗╸ Documentation: https://william278.net/docs/velocitab""")
public class Settings {

    @Getter
    @YamlKey("check_for_updates")
    @YamlComment("Check for updates on startup")
    private boolean checkForUpdates = true;

    @YamlKey("headers")
    @YamlComment("Header(s) to display above the TAB list for each server group.\nList multiple headers and set update_rate to the number of ticks between frames for basic animations")
    private Map<String, List<String>> headers = Map.of("default", List.of("&rainbow&Running Velocitab by William278"));

    @YamlKey("footers")
    @YamlComment("Footer(s) to display below the TAB list for each server group, same as headers.")
    private Map<String, List<String>> footers = Map.of("default", List.of("[There are currently %players_online%/%max_players_online% players online](gray)"));

    @YamlKey("formats")
    private Map<String, String> formats = Map.of("default", "&7[%server%] &f%prefix%%username%");

    @Getter
    @YamlComment("Which text formatter to use (MINEDOWN, MINIMESSAGE, or LEGACY)")
    @YamlKey("formatting_type")
    private Formatter formatter = Formatter.MINEDOWN;

    @Getter
    @YamlKey("server_groups")
    @YamlComment("The servers in each group of servers")
    private Map<String, List<String>> serverGroups = Map.of("default", List.of("lobby1", "lobby2", "lobby3"));

    @Getter
    @YamlKey("fallback_enabled")
    @YamlComment("All servers which are not in other groups will be put in the fallback group.\n\"false\" will exclude them from Velocitab.")
    private boolean fallbackEnabled = true;

    @Getter
    @YamlKey("fallback_group")
    @YamlComment("The formats to use for the fallback group.")
    private String fallbackGroup = "default";

    @Getter
    @YamlKey("only_list_players_in_same_group")
    @YamlComment("Only show other players on a server that is part of the same server group as the player.")
    private boolean onlyListPlayersInSameGroup = true;

    @Getter
    @YamlKey("server_display_names")
    @YamlComment("Define custom names to be shown in the TAB list for specific server names.\n" +
            "If no custom display name is provided for a server, its original name will be used.")
    private Map<String, String> serverDisplayNames = Map.of("very-long-server-name", "VLSN");

    @Getter
    @YamlKey("enable_papi_hook")
    private boolean enablePapiHook = true;

    @Getter
    @YamlKey("enable_miniplaceholders_hook")
    @YamlComment("If you are using MINIMESSAGE formatting, enable this to support MiniPlaceholders in formatting.")
    private boolean enableMiniPlaceholdersHook = true;

    @Getter
    @YamlKey("sort_players")
    @YamlComment("Whether to sort players in the TAB list.")
    private boolean sortPlayers = true;

    @YamlKey("sort_players_by")
    @YamlComment("Ordered list of elements by which players should be sorted. (ROLE_WEIGHT, ROLE_NAME and SERVER are supported)")
    private List<String> sortPlayersBy = List.of(
            TabPlayer.SortableElement.ROLE_WEIGHT.name(),
            TabPlayer.SortableElement.ROLE_NAME.name()
    );

    @Getter
    @YamlKey("update_rate")
    @YamlComment("How often in milliseconds to periodically update the TAB list, including header and footer, for all users.\n" +
            "If set to 0, TAB will be updated on player join/leave instead. (1s = 1000ms)\n" +
            "The minimal update rate is 200ms, anything lower will automatically be set to 200ms.")
    private int updateRate = 0;

    public Settings(@NotNull Velocitab plugin) {
        this.serverGroups = Map.of("default",
                plugin.getServer().getAllServers().stream().map(server -> server.getServerInfo().getName()).toList()
        );
    }

    @SuppressWarnings("unused")
    public Settings() {
    }

    @NotNull
    public String getHeader(@NotNull String serverGroup, int index) {
        final List<String> groupHeaders = headers.getOrDefault(serverGroup, List.of(""));
        return groupHeaders.isEmpty() ? "" : StringEscapeUtils.unescapeJava(groupHeaders
                .get(Math.max(0, Math.min(index, getHeaderListSize(serverGroup) - 1))));
    }

    @NotNull
    public String getFooter(@NotNull String serverGroup, int index) {
        final List<String> groupFooters = footers.getOrDefault(serverGroup, List.of(""));
        return groupFooters.isEmpty() ? "" : StringEscapeUtils.unescapeJava(groupFooters
                .get(Math.max(0, Math.min(index, getFooterListSize(serverGroup) - 1))));
    }

    public int getHeaderListSize(@NotNull String serverGroup) {
        return headers.getOrDefault(serverGroup, List.of("")).size();
    }

    public int getFooterListSize(@NotNull String serverGroup) {
        return footers.getOrDefault(serverGroup, List.of("")).size();
    }

    @NotNull
    public String getFormat(@NotNull String serverGroup) {
        return StringEscapeUtils.unescapeJava(
                formats.getOrDefault(serverGroup, "%username%"));
    }

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

    /**
     * Get the server group that a server is in
     *
     * @param serverName The name of the server
     * @return The server group that the server is in, or "default" if the server is not in a group
     */
    @NotNull
    public String getServerGroup(String serverName) {
        return serverGroups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(serverName)).findFirst()
                .map(Map.Entry::getKey)
                .orElse(fallbackGroup);
    }

    @NotNull
    public List<TabPlayer.SortableElement> getSortingElementList() {
        return sortPlayersBy.stream()
                .map(p -> TabPlayer.SortableElement.parse(p).orElseThrow(() ->
                        new IllegalArgumentException("Invalid sorting element set in config file: " + p)))
                .toList();
    }

}
