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

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class TabPlayer implements Comparable<TabPlayer> {
    private static final int DEFAULT_LATENCY = 3;
    private final Player player;
    private final Role role;
    private final GameProfile profile;

    public TabPlayer(@NotNull Player player, @NotNull Role role) {
        this.player = player;
        this.role = role;
        final String profileName = role.getStringComparableWeight() + "-" + getServerName() + "-" + player.getUsername();
        this.profile = new GameProfile(
                new UUID(0, new Random().nextLong()),
                profileName.length() > 16 ? profileName.substring(0, 16) : profileName,
                player.getGameProfileProperties()
        );
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
    public GameProfile getProfile() {
        return profile;
    }

    @NotNull
    public String getServerName() {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("Unknown");
    }

    @NotNull
    private Component getDisplayName(@NotNull Velocitab plugin) {
        return new MineDown(Placeholder.format(plugin.getSettings().getFormat(), plugin, this)).toComponent();
    }

    private TabListEntry getEntry(@NotNull Velocitab plugin, @NotNull TabList tabList) {
        return TabListEntry.builder()
                .displayName(getDisplayName(plugin))
                .latency(DEFAULT_LATENCY)
                .profile(profile)
                .tabList(tabList)
                .build();
    }

    public void sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        this.player.sendPlayerListHeaderAndFooter(tabList.getHeader(this), tabList.getFooter(this));
    }

    public void addPlayer(@NotNull TabPlayer player, @NotNull Velocitab plugin) {
        this.player.getTabList().addEntry(player.getEntry(plugin, this.player.getTabList()));
        removeUuidPlayer(plugin, player.getPlayer().getUniqueId());
    }

    public void removePlayer(@NotNull TabPlayer player, @NotNull Velocitab plugin) {
        this.player.getTabList().removeEntry(player.getProfile().getId());
        removeUuidPlayer(plugin, player.getPlayer().getUniqueId());
    }

    public void removeUuidPlayer(@NotNull Velocitab plugin, @NotNull UUID... uuid) {
        plugin.getServer().getScheduler()
                .buildTask(plugin, () -> Arrays.stream(uuid).forEach(this.player.getTabList()::removeEntry))
                .delay(500, TimeUnit.MILLISECONDS)
                .schedule();
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
