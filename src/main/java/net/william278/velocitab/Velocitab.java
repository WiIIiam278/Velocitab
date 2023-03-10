package net.william278.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.william278.annotaml.Annotaml;
import net.william278.velocitab.config.Formatter;
import net.william278.velocitab.config.Settings;
import net.william278.velocitab.hook.Hook;
import net.william278.velocitab.hook.LuckPermsHook;
import net.william278.velocitab.hook.MiniPlaceholdersHook;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Plugin(id = "velocitab")
public class Velocitab {

    private Settings settings;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private PlayerTabList tabList;
    private List<Hook> hooks;
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
        logger.info("Successfully enabled Velocitab");
    }

    @NotNull
    public ProxyServer getServer() {
        return server;
    }

    @NotNull
    public Settings getSettings() {
        return settings;
    }

    @NotNull
    public Formatter getFormatter() {
        return getSettings().getFormatter();
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

    private <H extends Hook> Optional<H> getHook(@NotNull Class<H> hookType) {
        return hooks.stream()
                .filter(hook -> hook.getClass().equals(hookType))
                .map(hookType::cast)
                .findFirst();
    }

    public Optional<LuckPermsHook> getLuckPerms() {
        return getHook(LuckPermsHook.class);
    }

    public Optional<PapiHook> getPapiHook() {
        return getHook(PapiHook.class);
    }

    public Optional<MiniPlaceholdersHook> getMiniPlaceholdersHook() {
        return getHook(MiniPlaceholdersHook.class);
    }

    private void loadHooks() {
        this.hooks = new ArrayList<>();
        Hook.AVAILABLE.forEach(availableHook -> availableHook.apply(this).ifPresent(hooks::add));
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
