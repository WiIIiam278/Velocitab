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

package net.william278.velocitab.config;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import net.william278.desertwell.util.Version;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Interface for getting and setting data from plugin configuration files
 *
 * @since 1.0
 */
public interface ConfigProvider {

    @NotNull
    YamlConfigurationProperties.Builder<?> YAML_CONFIGURATION_PROPERTIES = YamlConfigurationProperties.newBuilder()
            .charset(StandardCharsets.UTF_8)
            .outputNulls(true)
            .inputNulls(false)
            .setNameFormatter(NameFormatters.LOWER_UNDERSCORE);

    @NotNull
    Velocitab getPlugin();

    /**
     * Get the plugin settings, read from the config file
     *
     * @return the plugin settings
     * @since 1.0
     */
    @NotNull
    Settings getSettings();

    /**
     * Set the plugin settings
     *
     * @param settings The settings to set
     * @since 1.0
     */
    void setSettings(@NotNull Settings settings);

    /**
     * Load the plugin settings from the config file
     *
     * @since 1.0
     */
    default void loadSettings() {
        setSettings(YamlConfigurations.update(
                getConfigDirectory().resolve("config.yml"),
                Settings.class,
                YAML_CONFIGURATION_PROPERTIES.header(Settings.CONFIG_HEADER).build()
        ));
        getSettings().validateConfig(getPlugin(), "config.yml");
    }

    /**
     * Get the tab groups
     *
     * @return the tab groups
     * @since 1.0
     */
    @NotNull
    TabGroupsManager getTabGroupsManager();

    /**
     * Set the tab groups
     *
     * @param tabGroupsManager The tab groups to set
     * @since 1.7.4
     */
    void setTabGroupsManager(@NotNull TabGroupsManager tabGroupsManager);

    /**
     * Load the tab groups from the config file
     *
     * @since 1.0
     */
    default void loadTabGroups() {
        setTabGroupsManager(new TabGroupsManager(getPlugin()));
        getTabGroupsManager().loadGroups();
    }

    /**
     * Load the tab groups from the config file
     *
     * @since 1.0
     */
    @NotNull
    default Metadata getMetadata() {
        final URL resource = ConfigProvider.class.getResource("/metadata.yml");
        try (InputStream input = Objects.requireNonNull(resource, "Metadata file missing").openStream()) {
            return YamlConfigurations.read(input, Metadata.class, YAML_CONFIGURATION_PROPERTIES.build());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load plugin metadata", e);
        }
    }

    @SuppressWarnings("OptionalIsPresent")
    default void checkCompatibility() {
        if (getSkipCompatibilityCheck().orElse(false)) {
            getPlugin().getLogger().warn("Skipping compatibility checks");
            return;
        }

        // Validate Velocity platform version
        final Metadata metadata = getMetadata();
        final Version proxyVersion = getVelocityVersion();
        metadata.validateApiVersion(proxyVersion);
        metadata.validateBuild(proxyVersion);

        // Validate PAPIProxyBridge hook version
        final Optional<Version> papiProxyBridgeVersion = getPapiProxyBridgeVersion();
        if (papiProxyBridgeVersion.isPresent()) {
            metadata.validatePapiProxyBridgeVersion(papiProxyBridgeVersion.get());
        }
    }

    @NotNull
    default Optional<Boolean> getSkipCompatibilityCheck() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .filter(s -> s.startsWith("-Dvelocitab.skip-compatibility-check="))
                .map(s -> s.substring(s.indexOf('=') + 1))
                .filter(s -> s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false"))
                .map(Boolean::parseBoolean)
                .findFirst();
    }

    default Optional<Version> getPapiProxyBridgeVersion() {
        return getPlugin().getServer().getPluginManager()
                .getPlugin("papiproxybridge").map(PluginContainer::getDescription)
                .flatMap(PluginDescription::getVersion).map(Version::fromString);
    }

    @NotNull
    Version getVelocityVersion();

    /**
     * Get the plugin config directory
     *
     * @return the plugin config directory
     * @since 1.0
     */
    @NotNull
    Path getConfigDirectory();

}
