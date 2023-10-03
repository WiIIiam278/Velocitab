/*
 * This file is part of Velocitab, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.velocitab;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import lombok.Getter;
import net.william278.annotaml.Annotaml;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.desertwell.util.Version;
import net.william278.velocitab.commands.VelocitabCommand;
import net.william278.velocitab.config.Formatter;
import net.william278.velocitab.config.Settings;
import net.william278.velocitab.hook.Hook;
import net.william278.velocitab.hook.LuckPermsHook;
import net.william278.velocitab.hook.MiniPlaceholdersHook;
import net.william278.velocitab.hook.PAPIProxyBridgeHook;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.player.Role;
import net.william278.velocitab.player.TabPlayer;
import net.william278.velocitab.sorting.SortingManager;
import net.william278.velocitab.tab.PlayerTabList;
import net.william278.velocitab.vanish.VanishManager;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Plugin(id = "velocitab")
public class Velocitab {
    private static final int METRICS_ID = 18247;
    private Settings settings;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    @Inject
    private PluginContainer pluginContainer;
    @Inject
    private Metrics.Factory metricsFactory;
    private PlayerTabList tabList;
    private List<Hook> hooks;
    private ScoreboardManager scoreboardManager;
    private SortingManager sortingManager;
    @Getter
    private VanishManager vanishManager;

    @Inject
    public Velocitab(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(@NotNull ProxyInitializeEvent event) {
        loadSettings();
        loadHooks();
        prepareSortingManager();
        prepareScoreboardManager();
        prepareTabList();
        prepareVanishManager();
        registerCommands();
        registerMetrics();
        checkForUpdates();
        logger.info("Successfully enabled Velocitab");
    }

    @Subscribe
    public void onProxyShutdown(@NotNull ProxyShutdownEvent event) {
        server.getScheduler().tasksByPlugin(this).forEach(ScheduledTask::cancel);
        disableScoreboardManager();
        getLuckPermsHook().ifPresent(LuckPermsHook::close);
        logger.info("Successfully disabled Velocitab");
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

    public void loadSettings() {
        try {
            settings = Annotaml.create(
                    new File(dataDirectory.toFile(), "config.yml"),
                    new Settings(this)
            ).get();

            settings.getNametags().values().stream()
                    .filter(nametag -> !nametag.contains("%username%")).forEach(nametag -> {
                        logger.warn("Nametag '" + nametag + "' does not contain %username% - removing");
                        settings.getNametags().remove(nametag);
                    });
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

    public Optional<LuckPermsHook> getLuckPermsHook() {
        return getHook(LuckPermsHook.class);
    }

    public Optional<PAPIProxyBridgeHook> getPAPIProxyBridgeHook() {
        return getHook(PAPIProxyBridgeHook.class);
    }

    public Optional<MiniPlaceholdersHook> getMiniPlaceholdersHook() {
        return getHook(MiniPlaceholdersHook.class);
    }

    private void loadHooks() {
        this.hooks = new ArrayList<>();
        Hook.AVAILABLE.forEach(availableHook -> availableHook.apply(this).ifPresent(hooks::add));
    }

    private void prepareSortingManager() {
        if (settings.isSortPlayers()) {
            this.sortingManager = new SortingManager(this);
        }
    }

    private void prepareScoreboardManager() {
        if (settings.isSortPlayers()) {
            this.scoreboardManager = new ScoreboardManager(this);
            scoreboardManager.registerPacket();
        }
    }

    private void disableScoreboardManager() {
        if (scoreboardManager != null && settings.isSortPlayers()) {
            scoreboardManager.unregisterPacket();
        }
    }

    private void prepareVanishManager() {
        this.vanishManager = new VanishManager(this);
    }

    @NotNull
    public Optional<ScoreboardManager> getScoreboardManager() {
        return Optional.ofNullable(scoreboardManager);
    }

    public Optional<SortingManager> getSortingManager() {
        return Optional.ofNullable(sortingManager);
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
                getLuckPermsHook().map(hook -> hook.getPlayerRole(player)).orElse(Role.DEFAULT_ROLE),
                getLuckPermsHook().map(LuckPermsHook::getHighestWeight).orElse(0)
        );
    }

    public Optional<TabPlayer> getTabPlayer(String name) {
        return server.getPlayer(name).map(this::getTabPlayer);
    }

    private void registerCommands() {
        final BrigadierCommand command = new VelocitabCommand(this).command();
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder(command).plugin(this).build(),
                command
        );
    }

    @NotNull
    public PluginDescription getDescription() {
        return pluginContainer.getDescription();
    }

    @NotNull
    public Version getVersion() {
        return Version.fromString(getDescription().getVersion().orElseThrow(), "-");
    }

    private void registerMetrics() {
        final Metrics metrics = metricsFactory.make(this, METRICS_ID);
        metrics.addCustomChart(new SimplePie("sort_players", () -> settings.isSortPlayers() ? "Enabled" : "Disabled"));
        metrics.addCustomChart(new SimplePie("formatter_type", () -> settings.getFormatter().getName()));
        metrics.addCustomChart(new SimplePie("using_luckperms", () -> getLuckPermsHook().isPresent() ? "Yes" : "No"));
        metrics.addCustomChart(new SimplePie("using_papiproxybridge", () -> getPAPIProxyBridgeHook().isPresent() ? "Yes" : "No"));
        metrics.addCustomChart(new SimplePie("using_miniplaceholders", () -> getMiniPlaceholdersHook().isPresent() ? "Yes" : "No"));

    }

    private void checkForUpdates() {
        if (!getSettings().isCheckForUpdates()) {
            return;
        }
        getUpdateChecker().check().thenAccept(checked -> {
            if (!checked.isUpToDate()) {
                log(Level.WARN, "A new version of Velocitab is available: " + checked.getLatestVersion());
            }
        });
    }

    @NotNull
    public UpdateChecker getUpdateChecker() {
        return UpdateChecker.builder()
                .currentVersion(getVersion())
                .endpoint(UpdateChecker.Endpoint.MODRINTH)
                .resource("velocitab")
                .build();
    }

    public void log(@NotNull Level level, @NotNull String message, @NotNull Throwable... exceptions) {
        switch (level) {
            case ERROR -> {
                if (exceptions.length > 0) {
                    logger.error(message, exceptions[0]);
                } else {
                    logger.error(message);
                }
            }
            case WARN -> {
                if (exceptions.length > 0) {
                    logger.warn(message, exceptions[0]);
                } else {
                    logger.warn(message);
                }
            }
            case INFO -> logger.info(message);
        }
    }

    public void log(@NotNull String message) {
        this.log(Level.INFO, message);
    }

}
