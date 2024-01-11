package net.william278.velocitab.tab;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.player.TabPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TabListListener {

    private final Velocitab plugin;
    private final PlayerTabList tabList;

    public TabListListener(Velocitab plugin) {
        this.plugin = plugin;
        this.tabList = plugin.getTabList();
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        event.getPlayer().getTabList().clearAll();
        event.getPlayer().getTabList().clearHeaderAndFooter();
        tabList.justKicked.add(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void onPlayerJoin(@NotNull ServerPostConnectEvent event) {
        final Player joined = event.getPlayer();
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(joined));

        final String serverName = joined.getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName)
                .orElse("");
        final Group group = tabList.getGroup(serverName);
        final boolean isDefault = !group.servers().contains(serverName);

        // If the server is not in a group, use fallback.
        // If fallback is disabled, permit the player to switch excluded servers without a header or footer override
        if (isDefault && !plugin.getSettings().isFallbackEnabled()) {
            event.getPlayer().sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            tabList.players.remove(event.getPlayer().getUniqueId());
            return;
        }

        tabList.joinPlayer(joined, group);
    }
    @Subscribe(order = PostOrder.LAST)
    public void onPlayerQuit(@NotNull DisconnectEvent event) {
        if (event.getLoginStatus() != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) {
            return;
        }

        // Remove the player from the tracking list, Print warning if player was not removed
        final UUID uuid = event.getPlayer().getUniqueId();
        final TabPlayer tabPlayer = tabList.players.get(uuid);
        if (tabPlayer == null) {
            plugin.log(String.format("Failed to remove disconnecting player %s (UUID: %s)",
                    event.getPlayer().getUsername(), uuid));
        }

        // Remove the player from the tab list of all other players
        plugin.getServer().getAllPlayers().forEach(player -> player.getTabList().removeEntry(uuid));

        // Update the tab list of all players
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> tabList.players.values().forEach(player -> {
                    player.getPlayer().getTabList().removeEntry(uuid);
                    player.sendHeaderAndFooter(tabList);
                }))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
        // Delete player team
        plugin.getScoreboardManager().ifPresent(manager -> manager.resetCache(event.getPlayer()));
        //remove player from tab list cache
        tabList.justKicked.remove(uuid);
    }

    @Subscribe
    public void proxyReload(@NotNull ProxyReloadEvent event) {
        plugin.loadConfigs();
        tabList.reloadUpdate();
        plugin.log("Velocitab has been reloaded!");
    }

}
