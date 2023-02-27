package net.william278.velocitab.config;

import de.exlll.configlib.Configuration;
import net.william278.velocitab.BuildConstants;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@Configuration
public class Settings {

    private String header = "&rainbow&Running Velocitab v" + BuildConstants.VERSION + " by William278";
    private String footer = "[There are currently %players_online%/%max_players_online% players online](gray)";
    private String format = "&7[%server%] &f%prefix%%username%";
    private ArrayList<String> excludedServers = new ArrayList<>();

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

    public boolean isServerExcluded(@NotNull String serverName) {
        return excludedServers.contains(serverName);
    }

}
