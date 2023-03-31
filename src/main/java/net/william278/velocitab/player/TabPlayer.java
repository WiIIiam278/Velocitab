package net.william278.velocitab.player;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public final class TabPlayer implements Comparable<TabPlayer> {
    private final Player player;
    private final Role role;
    private final int highestWeight;
    private int headerIndex = 0;
    private int footerIndex = 0;

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

    /**
     * Get the server name the player is currently on.
     * Isn't affected by server aliases defined in the config.
     *
     * @return The server name
     */
    @NotNull
    public String getServerName() {
        return player.getCurrentServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("unknown");
    }

    /**
     * Get the TAB server group this player is connected to
     * @param plugin instance of the {@link Velocitab} plugin
     * @return the name of the server group the player is on
     */
    @NotNull
    public String getServerGroup(@NotNull Velocitab plugin) {
        return plugin.getSettings().getServerGroup(this.getServerName());
    }

    /**
     * Get the display name of the server the player is currently on.
     * Affected by server aliases defined in the config.
     *
     * @param plugin The plugin instance
     * @return The display name of the server
     */
    @NotNull
    public String getServerDisplayName(@NotNull Velocitab plugin) {
        return plugin.getSettings().getServerDisplayName(getServerName());
    }

    @NotNull
    public CompletableFuture<Component> getDisplayName(@NotNull Velocitab plugin) {
        final String serverGroup = plugin.getSettings().getServerGroup(getServerName());
        return Placeholder.replace(plugin.getSettings().getFormat(serverGroup), plugin, this)
                .thenApply(formatted -> plugin.getFormatter().format(formatted, this, plugin));

    }

    @NotNull
    public String getTeamName(@NotNull Velocitab plugin) {
        return plugin.getSettings().getSortingElementList().stream()
                .map(element -> element.resolve(this, plugin))
                .collect(Collectors.joining("-"));
    }

    public void sendHeaderAndFooter(@NotNull PlayerTabList tabList) {
        tabList.getHeader(this).thenAccept(header -> tabList.getFooter(this)
                .thenAccept(footer -> player.sendPlayerListHeaderAndFooter(header, footer)));
    }

    public int getHeaderIndex() {
        return headerIndex;
    }

    public void incrementHeaderIndex(@NotNull Velocitab plugin) {
        if (headerIndex >= plugin.getSettings().getHeaderListSize(getServerGroup(plugin))) {
            headerIndex = 0;
            return;
        }
        headerIndex++;
    }

    public int getFooterIndex() {
        return footerIndex;
    }

    public void incrementFooterIndex(@NotNull Velocitab plugin) {
        if (footerIndex >= plugin.getSettings().getFooterListSize(getServerGroup(plugin))) {
            footerIndex = 0;
            return;
        }
        footerIndex++;
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

    /**
     * Elements for sorting players
     */
    @SuppressWarnings("unused")
    public enum SortableElement {
        ROLE_WEIGHT((player, plugin) -> player.getRole().getWeightString(player.highestWeight)),
        ROLE_NAME((player, plugin) -> player.getRole().getName()
                .map(name -> name.length() > 3 ? name.substring(0, 3) : name)
                .orElse("")),
        SERVER_NAME((player, plugin) -> player.getServerName());

        private final BiFunction<TabPlayer, Velocitab, String> elementResolver;

        SortableElement(@NotNull BiFunction<TabPlayer, Velocitab, String> elementResolver) {
            this.elementResolver = elementResolver;
        }

        @NotNull
        private String resolve(@NotNull TabPlayer tabPlayer, @NotNull Velocitab plugin) {
            return elementResolver.apply(tabPlayer, plugin);
        }

        public static Optional<SortableElement> parse(@NotNull String s) {
            return Arrays.stream(values()).filter(element -> element.name().equalsIgnoreCase(s)).findFirst();
        }
    }

}
