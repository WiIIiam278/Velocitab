package net.william278.velocitab.config;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

public enum Placeholder {

    PLAYERS_ONLINE((plugin, player) -> Integer.toString(plugin.getServer().getPlayerCount())),
    MAX_PLAYERS_ONLINE((plugin, player) -> Integer.toString(plugin.getServer().getConfiguration().getShowMaxPlayers())),
    LOCAL_PLAYERS_ONLINE((plugin, player) -> player.getPlayer().getCurrentServer()
            .map(ServerConnection::getServer)
            .map(RegisteredServer::getPlayersConnected)
            .map(players -> Integer.toString(players.size()))
            .orElse("")),
    CURRENT_DATE((plugin, player) -> DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDateTime.now())),
    CURRENT_TIME((plugin, player) -> DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.now())),
    USERNAME((plugin, player) -> escape(player.getPlayer().getUsername())),
    SERVER((plugin, player) -> player.getServerName()),
    PING((plugin, player) -> Long.toString(player.getPlayer().getPing())),
    PREFIX((plugin, player) -> player.getRole().getPrefix().orElse("")),
    SUFFIX((plugin, player) -> player.getRole().getSuffix().orElse("")),
    ROLE((plugin, player) -> player.getRole().getName().orElse("")),
    DEBUG_TEAM_NAME((plugin, player) -> escape(player.getTeamName()));

    private final BiFunction<Velocitab, TabPlayer, String> formatter;

    Placeholder(@NotNull BiFunction<Velocitab, TabPlayer, String> formatter) {
        this.formatter = formatter;
    }

    @NotNull
    public static String format(@NotNull String format, @NotNull Velocitab plugin, @NotNull TabPlayer player) {
        for (Placeholder placeholder : values()) {
            format = format.replace("%" + placeholder.name().toLowerCase() + "%", placeholder.formatter.apply(plugin, player));
        }

        return format;
    }

    private static String escape(String replace) {
        // Replace __ so that it is not seen as underline when the string is formatted.
        return replace.replace("__", "_\\_");
    }
}
