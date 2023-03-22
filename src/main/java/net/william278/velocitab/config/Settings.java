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
        ┗╸ Placeholders: %players_online%, %max_players_online%, %local_players_online%, %current_date%, %current_time%, %username%, %server%, %ping%, %prefix%, %suffix%, %role%""")
public class Settings {

    @YamlKey("headers")
    @YamlComment("Header(s) to display above the TAB list for each server group.\nList multiple headers and set update_rate to the number of ticks between frames for basic animations")
    private Map<String, List<String>> headers = Map.of("default", List.of("&rainbow&Running Velocitab by William278"));

    @YamlKey("footers")
    @YamlComment("Footer(s) to display below the TAB list for each server group, same as headers.")
    private Map<String, List<String>> footers = Map.of("default", List.of("[There are currently %players_online%/%max_players_online% players online](gray)"));

    @YamlKey("formats")
    private Map<String, String> formats = Map.of("default", "&7[%server%] &f%prefix%%username%");

    @Getter
    @YamlComment("Which text formatter to use (MINEDOWN or MINIMESSAGE)")
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

    @YamlKey("enable_papi_hook")
    private boolean enablePapiHook = true;

    @YamlKey("enable_miniplaceholders_hook")
    @YamlComment("If you are using MINIMESSAGE formatting, enable this to support MiniPlaceholders in formatting.")
    private boolean enableMiniPlaceholdersHook = true;

    @YamlKey("sort_players_by")
    @YamlComment("Ordered list of elements by which players should be sorted. (ROLE_WEIGHT, ROLE_NAME and SERVER are supported)")
    private List<String> sortPlayersBy = List.of(
            TabPlayer.SortableElement.ROLE_WEIGHT.name(),
            TabPlayer.SortableElement.ROLE_NAME.name()
    );

    @YamlKey("update_rate")
    @YamlComment("How often to periodically update the TAB list, including header and footer, for all users.\nWill only update on player join/leave if set to 0.")
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
    public String getHeader(@NotNull String serverGroup, @NotNull int index) {
        return StringEscapeUtils.unescapeJava(
                headers.getOrDefault(serverGroup, List.of("")).get(index));
    }

    @NotNull
    public String getFooter(@NotNull String serverGroup, @NotNull int index) {
        return StringEscapeUtils.unescapeJava(
                footers.getOrDefault(serverGroup, List.of("")).get(index));
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
     * Get the server group that a server is in
     *
     * @param serverName The name of the server
     * @return The server group that the server is in, or "default" if the server is not in a group
     */
    @NotNull
    public String getServerGroup(String serverName) {
        return serverGroups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(serverName)).findFirst()
                .map(Map.Entry::getKey).orElse(fallbackGroup);
    }

    public boolean isPapiHookEnabled() {
        return enablePapiHook;
    }

    public boolean isMiniPlaceholdersHookEnabled() {
        return enableMiniPlaceholdersHook;
    }

    @NotNull
    public List<TabPlayer.SortableElement> getSortingElementList() {
        return sortPlayersBy.stream()
                .map(p -> TabPlayer.SortableElement.parse(p).orElseThrow(() ->
                        new IllegalArgumentException("Invalid sorting element set in config file: " + p)))
                .toList();
    }

    public int getUpdateRate() {
        return updateRate;
    }

}
