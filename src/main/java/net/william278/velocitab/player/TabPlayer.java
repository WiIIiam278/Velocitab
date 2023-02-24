package net.william278.velocitab.player;

import com.velocitypowered.api.proxy.Player;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;

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
    public Component getDisplayName(@NotNull Velocitab plugin) {
        return new MineDown(Placeholder.format(plugin.getSettings().getFormat(), plugin, this)).toComponent();
    }

    @NotNull
    public String getTeamName() {
        return role.getStringComparableWeight(highestWeight) + "-" + getServerName() + "-" + player.getUsername();
    }

    public void sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        this.player.sendPlayerListHeaderAndFooter(tabList.getHeader(this), tabList.getFooter(this));
    }

    @Override
    public int compareTo(@NotNull TabPlayer o) {
        final int roleDifference = role.compareTo(o.role);
        if (roleDifference == 0) {
            return player.getUsername().compareTo(o.player.getUsername());
        }
        return roleDifference;
    }

}
