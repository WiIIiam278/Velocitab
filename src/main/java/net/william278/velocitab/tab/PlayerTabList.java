package net.william278.velocitab.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class PlayerTabList {
    private final Velocitab plugin;
    private final List<TabPlayer> players;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = new ArrayList<>();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        // Remove previous Tab entries for players when they move servers
        if (event.getPreviousServer() != null) {
            removePlayer(event.getPlayer());
        }

        final TabPlayer player = plugin.getTabPlayer(event.getPlayer());

        // Reset existing tab list
        player.getPlayer().getTabList().clearHeaderAndFooter();
        if (!player.getPlayer().getTabList().getEntries().isEmpty()) {
            player.getPlayer().getTabList().getEntries().clear();
        }

        // Show existing list to new player
        players.forEach(listPlayer -> player.addPlayer(listPlayer, plugin));
        addPlayer(player);
        refreshHeaderAndFooter();
    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        try {
            removePlayer(event.getPlayer());
            refreshHeaderAndFooter();
        } catch (Exception ignored) {
            // Ignore when server shutting down
        }
    }

    @NotNull
    public Component getHeader(@NotNull TabPlayer player) {
        return new MineDown(Placeholder.format(plugin.getSettings().getHeader(), plugin, player)).toComponent();
    }

    @NotNull
    public Component getFooter(@NotNull TabPlayer player) {
        return new MineDown(Placeholder.format(plugin.getSettings().getFooter(), plugin, player)).toComponent();
    }

    // Add a new tab player to the list and update for online players
    public void addPlayer(@NotNull TabPlayer player) {
        players.add(player);
        players.forEach(tabPlayer -> tabPlayer.addPlayer(player, plugin));
    }

    public void removePlayer(@NotNull Player playerToRemove) {
        final Optional<TabPlayer> quitTabPlayer = players.stream()
                .filter(player -> player.getPlayer().equals(playerToRemove)).findFirst();
        if (quitTabPlayer.isPresent()) {
            players.remove(quitTabPlayer.get());
            players.forEach(tabPlayer -> tabPlayer.removePlayer(quitTabPlayer.get(), plugin));
        }
    }

    public void refreshHeaderAndFooter() {
        players.forEach(tabPlayer -> tabPlayer.sendHeaderAndFooter(this));
    }
}
