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
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.Setter;
import net.william278.desertwell.util.UpdateChecker;
import net.william278.desertwell.util.Version;
import net.william278.velocitab.api.PluginMessageAPI;
import net.william278.velocitab.api.VelocitabAPI;
import net.william278.velocitab.commands.VelocitabCommand;
import net.william278.velocitab.config.ConfigProvider;
import net.william278.velocitab.config.Formatter;
import net.william278.velocitab.config.Settings;
import net.william278.velocitab.config.TabGroups;
import net.william278.velocitab.hook.Hook;
import net.william278.velocitab.hook.LuckPermsHook;
import net.william278.velocitab.hook.MiniPlaceholdersHook;
import net.william278.velocitab.packet.PacketEventManager;
import net.william278.velocitab.packet.ScoreboardManager;
import net.william278.velocitab.placeholder.PlaceholderManager;
import net.william278.velocitab.providers.HookProvider;
import net.william278.velocitab.providers.LoggerProvider;
import net.william278.velocitab.providers.MetricProvider;
import net.william278.velocitab.providers.ScoreboardProvider;
import net.william278.velocitab.sorting.SortingManager;
import net.william278.velocitab.tab.PlayerTabList;
import net.william278.velocitab.vanish.VanishManager;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.nio.file.Path;
import java.util.List;

@Plugin(id = "velocitab")
@Getter
public class Velocitab implements ConfigProvider, ScoreboardProvider, LoggerProvider, HookProvider, MetricProvider {

    @Setter
    private Settings settings;
    @Setter
    private TabGroups tabGroups;

    private final ProxyServer server;
    private final Logger logger;
    private final Path configDirectory;
    @Inject
    private PluginContainer pluginContainer;
    @Inject
    private Metrics.Factory metricsFactory;
    @Setter
    private PlayerTabList tabList;
    @Setter
    private List<Hook> hooks;
    @Setter
    private ScoreboardManager scoreboardManager;
    @Setter
    private SortingManager sortingManager;
    private VanishManager vanishManager;
    private PacketEventManager packetEventManager;
    private PluginMessageAPI pluginMessageAPI;
    private PlaceholderManager placeholderManager;

    @Inject
    public Velocitab(@NotNull ProxyServer server, @NotNull Logger logger, @DataDirectory Path configDirectory) {
        this.server = server;
        this.logger = logger;
        this.configDirectory = configDirectory;
    }

    @Subscribe
    public void onProxyInitialization(@NotNull ProxyInitializeEvent event) {
        checkCompatibility();
        loadConfigs();
        loadHooks();
        preparePlaceholderManager();
        prepareVanishManager();
        prepareChannelManager();
        prepareScoreboard();
        registerCommands();
        registerMetrics();
        checkForUpdates();
        prepareAPI();
        logger.info("Successfully enabled Velocitab");
    }

    @Subscribe
    public void onProxyShutdown(@NotNull ProxyShutdownEvent event) {
        disableScoreboardManager();
        getLuckPermsHook().ifPresent(LuckPermsHook::closeEvent);
        getMiniPlaceholdersHook().ifPresent(MiniPlaceholdersHook::unregisterExpansion);
        unregisterAPI();
        logger.info("Successfully disabled Velocitab");
    }

    @NotNull
    public Formatter getFormatter() {
        return getSettings().getFormatter();
    }

    public void loadConfigs() {
        loadSettings();
        loadTabGroups();
    }

    private void prepareVanishManager() {
        this.vanishManager = new VanishManager(this);
    }

    private void prepareChannelManager() {
        this.packetEventManager = new PacketEventManager(this);
    }

    private void preparePlaceholderManager() {
        this.placeholderManager = new PlaceholderManager(this);
    }

    @Override
    @NotNull
    public Velocitab getPlugin() {
        return this;
    }

    @Override
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    private void prepareAPI() {
        VelocitabAPI.register(this);
        if (settings.isEnablePluginMessageApi()) {
            pluginMessageAPI = new PluginMessageAPI(this);
            pluginMessageAPI.registerChannel();
            getLogger().info("Registered Velocitab Plugin Message API");
        }
        getLogger().info("Registered Velocitab API");
    }

    private void unregisterAPI() {
        VelocitabAPI.unregister();
        if (pluginMessageAPI != null) {
            pluginMessageAPI.unregisterChannel();
        }
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
    public Version getVelocityVersion() {
        return Version.fromString(server.getVersion().getVersion(), "-");
    }

    @NotNull
    public Version getVersion() {
        return Version.fromString(getDescription().getVersion().orElseThrow(), "-");
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

}
