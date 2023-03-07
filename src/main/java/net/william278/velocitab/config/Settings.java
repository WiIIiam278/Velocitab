package net.william278.velocitab.config;

import lombok.NoArgsConstructor;
import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.velocitab.BuildConstants;
import net.william278.velocitab.Velocitab;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@NoArgsConstructor
@YamlFile(header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃       Velocitab Config       ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┗╸ Placeholders: %players_online%, %max_players_online%, %local_players_online%, %current_date%, %current_time%, %username%, %server%, %ping%, %prefix%, %suffix%, %role%""")
public class Settings {

    @YamlKey("headers")
    @YamlComment("The header to display on the tab list per group.")
    private Map<String, String> headers = Map.of("default", "&rainbow&Running Velocitab v" + BuildConstants.VERSION + " by William278");
    @YamlKey("footers")
    @YamlComment("The footer to display on the tab list per group.")
    private Map<String, String> footers = Map.of("default", "[There are currently %players_online%/%max_players_online% players online](gray)");
    @YamlKey("formats")
    @YamlComment("How the player will appear on the tab list per group.")
    private Map<String, String> formats = Map.of("default", "&7[%server%] &f%prefix%%username%");
    @YamlKey("server_groups")
    @YamlComment("The servers in each group\nAll servers not defined in a group will be excluded from Velocitab")
    private Map<String, List<String>> serverGroups = Map.of("default", List.of("lobby1", "lobby2", "lobby3"));
    @YamlKey("fallback_group")
    @YamlComment("The group of servers that not defined in server_groups. \"excluded\" will exclude them from Velocitab.")
    private String fallbackGroup = "default";
    @YamlKey("update_rate")
    private int updateRate = 0;

    public Settings(@NotNull Velocitab plugin) {
        this.serverGroups = Map.of("default",
                plugin.getServer().getAllServers().stream().map(server -> server.getServerInfo().getName()).toList()
        );
    }

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

    @NotNull
    public Optional<List<String>> getBrotherServers(String serverName) {
        return serverGroups.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(fallbackGroup))// Exclude the fallback group
                .map(Map.Entry::getValue)// Get the members of each group
                .filter(servers -> servers.contains(serverName))// Find siblings of the server
                .findFirst()
                .or(() -> {
                    if (fallbackGroup.equals("excluded")) {// If the fallback group is excluded, return empty
                        return Optional.empty();
                    }
                    serverGroups.compute(fallbackGroup, (k, v) -> {
                        if (v == null) {// If the fallback group is empty, create it
                            return List.of(serverName);
                        }
                        if (!v.contains(serverName)) {
                            v.add(serverName);
                            return v; // If the fallback group is not empty, add the server to it
                        }
                        return v;
                    });

                    return Optional.of(serverGroups.get(fallbackGroup));
                });
    }

    public int getUpdateRate() {
        return updateRate;
    }
}
