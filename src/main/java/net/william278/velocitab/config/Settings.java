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
    private Map<String, String> headers = Map.of("default", "&rainbow&Running Velocitab by William278");

    @YamlKey("footers")
    private Map<String, String> footers = Map.of("default", "[There are currently %players_online%/%max_players_online% players online](gray)");

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
    private List<TabPlayer.SortableElement> sortPlayersBy = List.of(
            TabPlayer.SortableElement.ROLE_WEIGHT,
            TabPlayer.SortableElement.ROLE_NAME
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
    public String getHeader(@NotNull String serverGroup) {
        return StringEscapeUtils.unescapeJava(
                headers.getOrDefault(serverGroup, ""));
    }

    @NotNull
    public String getFooter(@NotNull String serverGroup) {
        return StringEscapeUtils.unescapeJava(
                footers.getOrDefault(serverGroup, ""));
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
        return sortPlayersBy;
    }

    public int getUpdateRate() {
        return updateRate;
    }

}
