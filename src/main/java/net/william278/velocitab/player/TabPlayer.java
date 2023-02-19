package net.william278.velocitab.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.util.GameProfile;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;

public record TabPlayer(@NotNull Player player, @NotNull Role role) implements Comparable<TabPlayer> {
    @NotNull
    private Component getFormattedEntry(@NotNull Velocitab plugin) {
        return new MineDown(Placeholder.format(plugin.getSettings().getFormat(), plugin, this)).toComponent();
    }

    private TabListEntry getEntry(@NotNull Velocitab plugin, @NotNull TabList list) {
        return TabListEntry.builder()
                .displayName(getFormattedEntry(plugin))
                .latency((int) player.getPing())
                .tabList(list)
                .profile(new GameProfile(player.getUniqueId(),
                        role.getStringComparableWeight() + " " + player.getUsername(),
                        player.getGameProfileProperties()))
                .build();
    }

    public void sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        this.player.sendPlayerListHeaderAndFooter(tabList.getHeader(this), tabList.getFooter(this));
    }

    public void addPlayer(@NotNull TabPlayer player, @NotNull Velocitab plugin) {
        this.player.getTabList().addEntry(player.getEntry(plugin, this.player.getTabList()));
    }

    public void removePlayer(@NotNull TabPlayer player) {
        this.player.getTabList().removeEntry(player.player().getUniqueId());
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
