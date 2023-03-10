package net.william278.velocitab.hook;

import com.velocitypowered.api.proxy.Player;
import net.william278.papiproxybridge.api.PlaceholderAPI;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PapiHook extends Hook {

    private final PlaceholderAPI api;

    public PapiHook(@NotNull Velocitab plugin) {
        super(plugin);
        this.api = PlaceholderAPI.getInstance();
    }

    public CompletableFuture<String> formatPapiPlaceholders(@NotNull String input, @NotNull Player player) {
        return api.formatPlaceholders(input, player.getUniqueId());
    }


}
