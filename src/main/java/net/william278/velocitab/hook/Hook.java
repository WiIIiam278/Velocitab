package net.william278.velocitab.hook;

import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class Hook {

    public static final List<Function<Velocitab, Optional<Hook>>> AVAILABLE = List.of(
            (plugin -> {
                if (isPluginAvailable(plugin, "luckperms")) {
                    try {
                        plugin.log("Successfully hooked into LuckPerms");
                        return Optional.of(new LuckPermsHook(plugin));
                    } catch (Exception e) {
                        plugin.log("LuckPerms hook was not loaded: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            }),
            (plugin -> {
                if (isPluginAvailable(plugin, "papiproxybridge") && plugin.getSettings().isPapiHookEnabled()) {
                    try {
                        plugin.log("Successfully hooked into PAPIProxyBridge");
                        return Optional.of(new PapiHook(plugin));
                    } catch (Exception e) {
                        plugin.log("PAPIProxyBridge hook was not loaded: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            }),
            (plugin -> {
                if (isPluginAvailable(plugin, "miniplaceholders") && plugin.getSettings().isMiniPlaceholdersHookEnabled()) {
                    try {
                        plugin.log("Successfully hooked into MiniPlaceholders");
                        return Optional.of(new MiniPlaceholdersHook(plugin));
                    } catch (Exception e) {
                        plugin.log("MiniPlaceholders hook was not loaded: " + e.getMessage(), e);
                    }
                }
                return Optional.empty();
            })
    );

    protected final Velocitab plugin;

    public Hook(@NotNull Velocitab plugin) {
        this.plugin = plugin;
    }

    private static boolean isPluginAvailable(@NotNull Velocitab plugin, @NotNull String id) {
        return plugin.getServer().getPluginManager().getPlugin(id).isPresent();
    }

}
