package net.william278.velocitab.player;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class TabPlayer implements Comparable<TabPlayer> {
    private final Player player;
    private final Role role;
    private final int highestWeight;

    public TabPlayer(@NotNull Player player, @NotNull Role role, int highestWeight) {
        this.player = player;
        this.role = role;
        this.highestWeight = highestWeight;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public Role getRole() {
        return role;
    }

    @NotNull
    public String getServerName() {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("Unknown");
    }

    @NotNull
    public CompletableFuture<Component> getDisplayName(@NotNull Velocitab plugin) {
        final String serverGroup = plugin.getSettings().getServerGroup(getServerName());
        return Placeholder.format(plugin.getSettings().getFormat(serverGroup), plugin, this)
                .thenApply(formatted -> plugin.getSettings().getFormatter().format(formatted, this, plugin));

    }

    @NotNull
    public String getTeamName() {
        return role.getWeightString(highestWeight) + role.getName().map(name -> "-" + name).orElse("");
    }

    public void sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        tabList.getHeader(this).thenAccept(header -> tabList.getFooter(this)
                .thenAccept(footer -> player.sendPlayerListHeaderAndFooter(header, footer)));
    }

    @Override
    public int compareTo(@NotNull TabPlayer o) {
        final int roleDifference = role.compareTo(o.role);
        if (roleDifference == 0) {
            return player.getUsername().compareTo(o.player.getUsername());
        }
        return roleDifference;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TabPlayer other && player.getUniqueId().equals(other.player.getUniqueId());
    }
}
