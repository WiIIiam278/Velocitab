package net.william278.velocitab.tab;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.proxy.server.ServerInfo;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
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
        plugin.getScoreboardManager().resetCache(joined);

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
        final TabPlayer tabPlayer = plugin.getTabPlayer(joined);
        players.add(tabPlayer);

        // Update lists
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> {
                    final TabList tabList = joined.getTabList();
                    final Map<String, String> playerRoles = new HashMap<>();
                    players.forEach(player -> {
                        playerRoles.put(player.getPlayer().getUsername(), player.getTeamName());
                        tabList.getEntries().stream()
                                .filter(e -> e.getProfile().getId().equals(player.getPlayer().getUniqueId())).findFirst()
                                .ifPresentOrElse(
                                        entry -> entry.setDisplayName(player.getDisplayName(plugin)),
                                        () -> tabList.addEntry(createEntry(player, tabList))
                                );
                        addPlayerToTabList(player, tabPlayer);
                        player.sendHeaderAndFooter(this);
                    });
                    plugin.getScoreboardManager().setRoles(joined, playerRoles);
                })
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @NotNull
    private TabListEntry createEntry(@NotNull TabPlayer player, @NotNull TabList tabList) {
        return TabListEntry.builder()
                .profile(player.getPlayer().getGameProfile())
                .displayName(player.getDisplayName(plugin))
                .latency(0)
                .tabList(tabList)
                .build();
    }

    private void addPlayerToTabList(@NotNull TabPlayer player, @NotNull TabPlayer newPlayer) {
        if (newPlayer.getPlayer().getUniqueId().equals(player.getPlayer().getUniqueId())) {
            return;
        }

        player.getPlayer()
                .getTabList().getEntries().stream()
                .filter(e -> e.getProfile().getId().equals(newPlayer.getPlayer().getUniqueId())).findFirst()
                .ifPresentOrElse(
                        entry -> entry.setDisplayName(newPlayer.getDisplayName(plugin)),
                        () -> player.getPlayer().getTabList()
                                .addEntry(createEntry(newPlayer, player.getPlayer().getTabList()))
                );
        plugin.getScoreboardManager().updateRoles(
                player.getPlayer(),
                newPlayer.getTeamName(),
                newPlayer.getPlayer().getUsername()
        );
    }

    @Subscribe
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        // Remove the player from the tracking list
        players.removeIf(player -> player.getPlayer().getUniqueId().equals(event.getPlayer().getUniqueId()));

        // Remove the player from the tab list of all other players
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(event.getPlayer().getUniqueId()));

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> players.forEach(player -> {
                    player.getPlayer().getTabList().removeEntry(event.getPlayer().getUniqueId());
                    player.sendHeaderAndFooter(this);
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
    }

    public void onPlayerRoleUpdate(@NotNull TabPlayer tabPlayer) {
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> players.forEach(player -> {
                    player.getPlayer().getTabList().getEntries().stream()
                            .filter(e -> e.getProfile().getId().equals(tabPlayer.getPlayer().getUniqueId())).findFirst()
                            .ifPresent(entry -> entry.setDisplayName(tabPlayer.getDisplayName(plugin)));
                    plugin.getScoreboardManager().updateRoles(player.getPlayer(),
                            tabPlayer.getTeamName(), tabPlayer.getPlayer().getUsername());
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
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
