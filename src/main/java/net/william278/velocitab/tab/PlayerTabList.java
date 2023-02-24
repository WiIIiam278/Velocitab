package net.william278.velocitab.tab;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class PlayerTabList {
    private final Velocitab plugin;
    private ImmutableList<TabPlayer> players;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = ImmutableList.copyOf(plugin.getServer().getAllPlayers().stream()
                .map(plugin::getTabPlayer)
                .toList());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final ArrayList<TabPlayer> players = new ArrayList<>(this.players);

        // Remove the player from the tracking list if they are switching servers
        if (event.getPreviousServer() == null) {
            players.removeIf(player -> player.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId()));
        }

        // Add the player to the tracking list
        players.add(plugin.getTabPlayer(event.getPlayer()));
        this.players = ImmutableList.copyOf(players);

        // Update the tab list of all players
        plugin.getServer().getScheduler().buildTask(plugin, this::updateList)
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        // Remove the player from the tracking list
        this.players = ImmutableList.copyOf(this.players.stream()
                .filter(player -> !player.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId()))
                .toList());

        // Remove the player from the tab list of all other players
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(event.getPlayer().getUniqueId()));

        // Update the tab list of all players
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
                    plugin.getScoreboardManager().removeTeam(event.getPlayer());
                    updateList();
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    public void updatePlayer(@NotNull TabPlayer tabPlayer) {
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    // Remove the existing player from the tracking list
                    this.players = ImmutableList.copyOf(this.players.stream()
                            .filter(player -> !player.getPlayer().getUniqueId().equals(tabPlayer.getPlayer().getUniqueId()))
                            .toList());

                    // Update the player's team sorting
                    plugin.getScoreboardManager().setPlayerTeam(tabPlayer);

                    updateList();
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    private void updateList() {
        this.players.forEach(player -> {
            player.sendHeaderAndFooter(this);
            player.getPlayer().getTabList().getEntries()
                    .forEach(entry -> this.players.stream()
                            .filter(p -> p.getPlayer().getGameProfile().getId().equals(entry.getProfile().getId()))
                            .findFirst().ifPresent(tabPlayer -> {
                                entry.setDisplayName(tabPlayer.getDisplayName(plugin));
                                plugin.getScoreboardManager().setPlayerTeam(tabPlayer);
                            }));
        });
    }

    @NotNull
    public Component getHeader(@NotNull TabPlayer player) {
        return new MineDown(Placeholder.format(plugin.getSettings().getHeader(), plugin, player)).toComponent();
    }

    @NotNull
    public Component getFooter(@NotNull TabPlayer player) {
        return new MineDown(Placeholder.format(plugin.getSettings().getFooter(), plugin, player)).toComponent();
    }

}
