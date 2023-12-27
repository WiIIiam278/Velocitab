package net.william278.velocitab.config;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TabGroups implements ConfigValidator {

    public static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      Velocitab TabGroups     ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/velocitab
            ┗╸ Documentation: https://william278.net/docs/velocitab""";

    public TreeMap<String, Group> groups = new TreeMap<>(Map.of(
            "default", new Group(
                    List.of("&rainbow&Running Velocitab by William278"),
                    List.of("[There are currently %players_online%/%max_players_online% players online](gray)"),
                    "&7[%server%] &f%prefix%%username%",
                    "&f%prefix%%username%&f%suffix%",
                    List.of("lobby", "survival", "creative", "minigames", "skyblock", "prison", "hub")
            )
    ));


    @NotNull
    public Group getGroupFromServers(@NotNull List<String> servers) {
        for (Group group : groups.values()) {
            if (group.servers().equals(servers)) {
                return group;
            }
        }
        return groups.get("default");
    }


    @Override
    public void validateConfig() {
        if (groups.isEmpty()) {
            throw new IllegalStateException("No tab groups defined in config");
        }
        if (!groups.containsKey("default")) {
            throw new IllegalStateException("No default tab group defined in config");
        }
    }
}
