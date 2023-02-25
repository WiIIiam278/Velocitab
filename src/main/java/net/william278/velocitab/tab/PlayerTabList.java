package net.william278.velocitab.tab;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class PlayerTabList {
    private final Velocitab plugin;
    private final ConcurrentLinkedQueue<TabPlayer> players;

    public PlayerTabList(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.players = new ConcurrentLinkedQueue<>();
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final Player joined = event.getPlayer();
        // Remove the player from the tracking list if they are switching servers
        if (event.getPreviousServer() == null) {
            players.removeIf(player -> player.getPlayer().getUniqueId().equals(joined.getUniqueId()));
        }

        // Don't set their list if they are on an excluded server
        if (plugin.getSettings().isServerExcluded(joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("?"))) {
            return;
        }

        // Add the player to the tracking list
        players.add(plugin.getTabPlayer(joined));

        // Update the tab list of all players
        plugin.getServer().getScheduler().buildTask(plugin, this::updateList)
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        // Remove the player from the tracking list
        players.removeIf(player -> player.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId()));

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
                    // Update the player's team sorting
                    players.remove(tabPlayer);
                    players.add(tabPlayer);

                    plugin.getScoreboardManager().setPlayerTeam(tabPlayer);

                    updateList();
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    private void updateList() {
        final ImmutableList<TabPlayer> players = ImmutableList.copyOf(this.players);
        players.forEach(player -> {
            player.sendHeaderAndFooter(this);

            // Fill the tab list with the players
            players.forEach(listedPlayer -> {
                final Optional<TabListEntry> current = player.getPlayer().getTabList().getEntries().stream()
                        .filter(entry -> entry.getProfile().getId().equals(listedPlayer.getPlayer().getUniqueId()))
                        .findFirst();
                current.ifPresentOrElse(
                        entry -> entry.setDisplayName(listedPlayer.getDisplayName(plugin)),
                        () -> player.getPlayer().getTabList().addEntry(TabListEntry.builder()
                                .profile(listedPlayer.getPlayer().getGameProfile())
                                .displayName(listedPlayer.getDisplayName(plugin))
                                .latency(0)
                                .tabList(player.getPlayer().getTabList())
                                .build()));
                plugin.getScoreboardManager().setPlayerTeam(listedPlayer);
            });

            // Remove players in the tab list that are not in the players list
            player.getPlayer().getTabList().getEntries().stream()
                    .filter(entry -> players.stream()
                            .noneMatch(listedPlayer -> listedPlayer.getPlayer().getUniqueId().equals(entry.getProfile().getId())))
                    .forEach(entry -> player.getPlayer().getTabList().removeEntry(entry.getProfile().getId()));
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
