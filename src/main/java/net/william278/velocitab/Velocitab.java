package net.william278.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.william278.annotaml.Annotaml;
import net.william278.velocitab.config.Settings;
import net.william278.velocitab.luckperms.LuckPermsHook;
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
                @Dependency(id = "luckperms", optional = true)
        }
)
public class Velocitab {

    private Settings settings;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private PlayerTabList tabList;
    private LuckPermsHook luckPerms;
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
            settings = Annotaml.create(new File(dataDirectory.toFile(), "config.yml"), Settings.class).get();
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            logger.error("Failed to load config file: " + e.getMessage(), e);
        }
    }

    public Optional<LuckPermsHook> getLuckPerms() {
        return Optional.ofNullable(luckPerms);
    }

    private void loadHooks() {
        if (server.getPluginManager().getPlugin("luckperms").isEmpty()) {
            return;
        }

        // If LuckPerms is present, load the hook
        try {
            luckPerms = new LuckPermsHook(this);
            server.getEventManager().register(this, luckPerms);
        } catch (IllegalArgumentException e) {
            logger.warn("LuckPerms was not loaded: " + e.getMessage(), e);
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
        server.getEventManager().register(this, new PlayerTabList(this));
    }

    @NotNull
    public TabPlayer getTabPlayer(@NotNull Player player) {
        return new TabPlayer(player, getLuckPerms().map(hook -> hook.getPlayerRole(player)).orElse(Role.DEFAULT_ROLE),
                getLuckPerms().map(LuckPermsHook::getHighestWeight).orElse(0));
    }

}
