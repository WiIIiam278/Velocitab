package net.william278.velocitab.luckperms;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.Player;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;
import java.util.UUID;

public class LuckPermsHook {

    private final Velocitab plugin;
    private final LuckPerms api;

    public LuckPermsHook(@NotNull Velocitab plugin) throws IllegalStateException {
        this.plugin = plugin;
        this.api = LuckPermsProvider.get();
    }

    @NotNull
    public Role getPlayerRole(@NotNull Player player) {
        final CachedMetaData metaData = getUser(player.getUniqueId()).getCachedData().getMetaData();
        final OptionalInt roleWeight = getWeight(metaData.getPrimaryGroup());
        if (roleWeight.isPresent()) {
            return new Role(
                    roleWeight.getAsInt(),
                    metaData.getPrimaryGroup(),
                    metaData.getPrefix(),
                    metaData.getSuffix()
            );
        }
        return Role.DEFAULT_ROLE;
    }

    @Subscribe
    public void onLuckPermsGroupUpdate(@NotNull UserDataRecalculateEvent event) {
        plugin.getServer().getPlayer(event.getUser().getUniqueId()).ifPresent(player -> {
            final PlayerTabList tabList = plugin.getTabList();
            tabList.removePlayer(player);
            tabList.addPlayer(plugin.getTabPlayer(player));
            tabList.refreshHeaderAndFooter();
        });
    }

    private OptionalInt getWeight(@Nullable String groupName) {
        final Group group;
        if (groupName == null || (group = api.getGroupManager().getGroup(groupName)) == null) {
            return OptionalInt.empty();
        }
        return group.getWeight();
    }

    private User getUser(@NotNull UUID uuid) {
        return api.getUserManager().getUser(uuid);
    }


}
