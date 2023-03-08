package net.william278.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.william278.annotaml.Annotaml;
import net.william278.velocitab.config.Settings;
import net.william278.velocitab.hook.LuckPermsHook;
import net.william278.velocitab.hook.PapiHook;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.tab.PlayerTabList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

@Plugin(
        id = "velocitab",
        name = "Velocitab",
        version = BuildConstants.VERSION,
        description = "Simple velocity TAB menu plugin",
        url = "https://william278.net/",
        authors = {"William278"},
        dependencies = {
                @Dependency(id = "protocolize"),
                @Dependency(id = "luckperms", optional = true),
                @Dependency(id = "papiproxybridge", optional = true)
        }
)
public class Velocitab {

    private Settings settings;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private PlayerTabList tabList;
    private LuckPermsHook luckPerms;
    private PapiHook papiHook;
    private ScoreboardManager scoreboardManager;

    @Inject
    public Velocitab(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadSettings();
        loadHooks();
        prepareScoreboardManager();
        prepareTabList();
        logger.info("Successfully enabled Velocitab v" + BuildConstants.VERSION);
    }

    @NotNull
    public ProxyServer getServer() {
        return server;
    }

    @NotNull
    public Settings getSettings() {
        return settings;
    }

    private void loadSettings() {
        try {
            settings = Annotaml.create(
                    new File(dataDirectory.toFile(), "config.yml"),
                    new Settings(this)
            ).get();
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            logger.error("Failed to load config file: " + e.getMessage(), e);
        }
    }

    public Optional<LuckPermsHook> getLuckPerms() {
        return Optional.ofNullable(luckPerms);
    }

    public Optional<PapiHook> getPapiHook() {
        return Optional.ofNullable(papiHook);
    }

    private void loadHooks() {
        // If LuckPerms is present, load the hook
        if (server.getPluginManager().getPlugin("luckperms").isPresent()) {
            try {
                luckPerms = new LuckPermsHook(this);
                logger.info("Successfully hooked into LuckPerms");
            } catch (IllegalArgumentException e) {
                logger.warn("LuckPerms was not loaded: " + e.getMessage(), e);
            }
        }

        // If PAPIProxyBridge is present, load the hook
        if (settings.isPapiHookEnabled() && server.getPluginManager().getPlugin("papiproxybridge").isPresent()) {
            try {
                papiHook = new PapiHook();
                logger.info("Successfully hooked into PAPIProxyBridge");
            } catch (IllegalArgumentException e) {
                logger.warn("PAPIProxyBridge was not loaded: " + e.getMessage(), e);
            }
        }
    }

    private void prepareScoreboardManager() {
        this.scoreboardManager = new ScoreboardManager(this);
        scoreboardManager.registerPacket();
    }

    @NotNull
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    @NotNull
    public PlayerTabList getTabList() {
        return tabList;
    }

    private void prepareTabList() {
        this.tabList = new PlayerTabList(this);
        server.getEventManager().register(this, tabList);
    }

    @NotNull
    public TabPlayer getTabPlayer(@NotNull Player player) {
        return new TabPlayer(player,
                getLuckPerms().map(hook -> hook.getPlayerRole(player))
                        .orElse(Role.DEFAULT_ROLE),
                getLuckPerms().map(LuckPermsHook::getHighestWeight)
                        .orElse(0));
    }

    public void log(@NotNull String message, @NotNull Throwable... exceptions) {
        Arrays.stream(exceptions).findFirst().ifPresentOrElse(
                exception -> logger.error(message, exception),
                () -> logger.warn(message)
        );
    }

}
