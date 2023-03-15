package net.william278.velocitab.config;

import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
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
    USERNAME((plugin, player) -> plugin.getFormatter().escape(player.getPlayer().getUsername())),
    SERVER((plugin, player) -> player.getServerName()),
    PING((plugin, player) -> Long.toString(player.getPlayer().getPing())),
    PREFIX((plugin, player) -> player.getRole().getPrefix().orElse("")),
    SUFFIX((plugin, player) -> player.getRole().getSuffix().orElse("")),
    ROLE((plugin, player) -> player.getRole().getName().orElse("")),
    DEBUG_TEAM_NAME((plugin, player) -> plugin.getFormatter().escape(player.getTeamName(plugin)));

    /**
     * Function to replace placeholders with a real value
     */
    private final BiFunction<Velocitab, TabPlayer, String> replacer;

    Placeholder(@NotNull BiFunction<Velocitab, TabPlayer, String> replacer) {
        this.replacer = replacer;
    }

    public static CompletableFuture<String> replace(@NotNull String format, @NotNull Velocitab plugin, @NotNull TabPlayer player) {
        for (Placeholder placeholder : values()) {
            format = format.replace("%" + placeholder.name().toLowerCase() + "%", placeholder.replacer.apply(plugin, player));
        }
        final String replaced = format;

        return plugin.getPapiHook()
                .map(hook -> hook.formatPapiPlaceholders(replaced, player.getPlayer()))
                .orElse(CompletableFuture.completedFuture(replaced));
    }

}
