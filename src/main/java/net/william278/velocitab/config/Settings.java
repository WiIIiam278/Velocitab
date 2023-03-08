package net.william278.velocitab.config;


import lombok.Getter;
import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.velocitab.BuildConstants;
import net.william278.velocitab.Velocitab;
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
    private Map<String, String> headers = Map.of("default", "&rainbow&Running Velocitab v" + BuildConstants.VERSION + " by William278");
    @YamlKey("footers")
    private Map<String, String> footers = Map.of("default", "[There are currently %players_online%/%max_players_online% players online](gray)");
    @YamlKey("formats")
    private Map<String, String> formats = Map.of("default", "&7[%server%] &f%prefix%%username%");
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
    @YamlComment("The format to use for the fallback group.")
    private String fallbackGroup = "default";
    @YamlKey("enable_papi_hook")
    private boolean enablePapiHook = true;
    @YamlKey(("update_rate"))
    private int updateRate = 0;

    public Settings(@NotNull Velocitab plugin) {
        this.serverGroups = Map.of("default",
                plugin.getServer().getAllServers().stream().map(server -> server.getServerInfo().getName()).toList()
        );
    }
    @SuppressWarnings("unused")
    public Settings(){}

    @NotNull
    public String getHeader(String serverGroup) {
        return StringEscapeUtils.unescapeJava(
                headers.getOrDefault(serverGroup, "&rainbow&Running Velocitab v" + BuildConstants.VERSION + " by William278"));
    }

    @NotNull
    public String getFooter(String serverGroup) {
        return StringEscapeUtils.unescapeJava(
                footers.getOrDefault(serverGroup, "[There are currently %players_online%/%max_players_online% players online](gray)"));
    }

    @NotNull
    public String getFormat(String serverGroup) {
        return StringEscapeUtils.unescapeJava(
                formats.getOrDefault(serverGroup, "&7[%server%] &f%prefix%%username%"));
    }

    /**
     * Get the server group that a server is in
     *
     * @param serverName The name of the server
     * @return The server group that the server is in, or "default" if the server is not in a group
     */
    public String getServerGroup(String serverName) {
        return serverGroups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(serverName)).findFirst()
                .map(Map.Entry::getKey).orElse(fallbackGroup);
    }

    public boolean isPapiHookEnabled() {
        return enablePapiHook;
    }

    public int getUpdateRate() {
        return updateRate;
    }
}
