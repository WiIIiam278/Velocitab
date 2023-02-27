package net.william278.velocitab.config;

import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;
import net.william278.velocitab.BuildConstants;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@YamlFile(header = """
        ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
        ┃       Velocitab Config       ┃
        ┃    Developed by William278   ┃
        ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
        ┗╸ Placeholders: %players_online%, %max_players_online%, %local_players_online%, %current_date%, %current_time%, %username%, %server%, %ping%, %prefix%, %suffix%, %role%""")
public class Settings {

    @YamlKey("header")
    private String header = "&rainbow&Running Velocitab v" + BuildConstants.VERSION + " by William278";
    @YamlKey("footer")
    private String footer = "[There are currently %players_online%/%max_players_online% players online](gray)";
    @YamlKey("format")
    private String format = "&7[%server%] &f%prefix%%username%";
    @YamlKey("server_groups")
    private Map<String, List<String>> serverGroups = Map.of("lobbies", List.of("lobby1", "lobby2", "lobby3"));

    private Settings() {
    }

    @NotNull
    public String getHeader() {
        return StringEscapeUtils.unescapeJava(header);
    }

    @NotNull
    public String getFooter() {
        return StringEscapeUtils.unescapeJava(footer);
    }

    @NotNull
    public String getFormat() {
        return StringEscapeUtils.unescapeJava(format);
    }

    @NotNull
    public Optional<List<String>> getServerGroup(String serverName) {
        return serverGroups.values().stream().filter(servers -> servers.contains(serverName)).findFirst();
    }

}
