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

public class PlayerTabList {
    private final Velocitab plugin;
    private final List<TabPlayer> players;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = new ArrayList<>();
    }

    @NotNull
    public Component getHeader(@NotNull TabPlayer player) {
        return new MineDown(Placeholder.format(plugin.getSettings().getHeader(), plugin, player)).toComponent();
    }

    @NotNull
    public Component getFooter(@NotNull TabPlayer player) {
        return new MineDown(Placeholder.format(plugin.getSettings().getFooter(), plugin, player)).toComponent();
    }


    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final TabPlayer player = plugin.getTabPlayer(event.getPlayer());

        // Show existing list to new player
        players.forEach(listPlayer -> player.addPlayer(listPlayer, plugin));

        // Update list for all players with new player
        players.add(player);
        players.forEach(tabPlayer -> {
            tabPlayer.addPlayer(player, plugin);
            tabPlayer.sendHeaderAndFooter(this);
        });
    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        final Player quitPlayer = event.getPlayer();
        final Optional<TabPlayer> quitTabPlayer = players.stream()
                .filter(player -> player.player().equals(quitPlayer)).findFirst();
        if (quitTabPlayer.isPresent()) {
            players.remove(quitTabPlayer.get());
            players.forEach(tabPlayer -> {
                tabPlayer.removePlayer(quitTabPlayer.get());
                tabPlayer.sendHeaderAndFooter(this);
            });
        }
    }

}
