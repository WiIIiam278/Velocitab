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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import net.jodah.expiringmap.ExpiringMap;
import net.william278.velocitab.Velocitab;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TabGroupsManager {

    private final Velocitab plugin;
    private final Map<String, Group> groups;
    private final Map<TabGroups, String> groupsFiles;
    private List<Group> groupsList;
    private final Map<Group, List<RegisteredServer>> cachedServers;

    public TabGroupsManager(@NotNull Velocitab plugin) {
        this.plugin = plugin;
        this.groups = Maps.newConcurrentMap();
        this.groupsFiles = Maps.newConcurrentMap();
        this.groupsList = Lists.newArrayList();
        this.cachedServers = ExpiringMap.builder()
                .expiration(30, TimeUnit.SECONDS)
                .build();
    }

    public void loadGroups() {
        groups.clear();
        groupsFiles.clear();
        cachedServers.clear();
        final Path configDirectory = plugin.getConfigDirectory();
        final File defaultFile = configDirectory.resolve("tab_groups.yml").toFile();
        final YamlConfigurationProperties properties = ConfigProvider.YAML_CONFIGURATION_PROPERTIES.header(TabGroups.CONFIG_HEADER).build();
        final TabGroups defaultTagGroupsFile = YamlConfigurations.update(
                configDirectory.resolve("tab_groups.yml"),
                TabGroups.class,
                properties
        );

        final String defaultName = defaultFile.getAbsolutePath().replace(".yml", "");
        if (!validateGroups(defaultTagGroupsFile, defaultName)) {
            throw new IllegalStateException("Failed to load default tab groups file");
        }

        final File folder = plugin.getConfigDirectory().resolve("tab_groups").toFile();
        if (folder.exists()) {
            final File[] filesArray = folder.listFiles();
            final List<File> files = filesArray == null ? List.of() : Arrays.asList(filesArray);
            for (File file : files) {
                if (!file.getName().endsWith(".yml")) {
                    continue;
                }

                final TabGroups preCheck = YamlConfigurations.load(file.toPath(), TabGroups.class, properties);
                preCheck.groups.removeIf(g -> g.name().equals("default"));
                YamlConfigurations.save(file.toPath(), TabGroups.class, preCheck, properties);

                final TabGroups group = YamlConfigurations.update(
                        file.toPath(),
                        TabGroups.class,
                        properties
                );
                final String name = folder.getAbsoluteFile() + "/" + file.getName().replace(".yml", "");
                if (!validateGroups(group, name)) {
                    throw new IllegalStateException("Failed to load tab groups file " + file.getName());
                }
            }
        }

        this.groupsList = Lists.newArrayList(getGroups());
    }

    @NotNull
    public List<File> getGroupsFiles() {
        return groupsFiles.values().stream().map(f -> new File(f+".yml")).collect(Collectors.toList());
    }

    public boolean isDefaultFile(@NotNull File file) {
        return plugin.getConfigDirectory().resolve("tab_groups.yml").toFile().getAbsolutePath().equals(file.getAbsolutePath());
    }

    private boolean validateGroups(@NotNull TabGroups group, @NotNull String name) {
        this.groupsFiles.put(group, name);
        group.validateConfig(plugin, name);

        final List<Group> eligibleGroups = Lists.newArrayList();
        final Set<RegisteredServer> registeredServers = Sets.newHashSet();

        outer:
        for (Group group1 : group.groups) {
            final List<RegisteredServer> current = group1.registeredServers(plugin, false);
            final boolean isDefault = group1.isDefault(plugin);

            if (groups.containsKey(group1.name())) {
                plugin.getLogger().warn("Group {} is already defined in {} tab groups file. Skipping.", group1.name(), name);
                continue;
            }

            if(!(isDefault && plugin.getSettings().isFallbackEnabled())) {
                for (RegisteredServer registeredServer : current) {
                    if (registeredServers.contains(registeredServer)) {
                        plugin.getLogger().warn("Server {} is already registered for group {} in {}, the same tabgroups file. Skipping.", registeredServer.getServerInfo().getName(), group1.name(), name);
                        continue outer;
                    }
                }
            }

            registeredServers.addAll(current);
            eligibleGroups.add(group1);
        }

        outer:
        for (Group group1 : groups.values()) {
            final List<RegisteredServer> current = group1.registeredServers(plugin, false);

            for (Group loadingGroup : eligibleGroups) {
                final List<RegisteredServer> loadingGroupServers = loadingGroup.registeredServers(plugin, false);
                final boolean isDefault = loadingGroup.isDefault(plugin);

                if(!(isDefault && plugin.getSettings().isFallbackEnabled())) {
                    for (RegisteredServer registeredServer : loadingGroupServers) {
                        if (current.contains(registeredServer)) {
                            plugin.getLogger().warn("Server {} in {} tab groups file is already registered for group {}. Skipping.", registeredServer.getServerInfo().getName(), name, group1.name());
                            eligibleGroups.remove(loadingGroup);
                            continue outer;
                        }
                    }
                }
            }
        }

        for (Group group1 : eligibleGroups) {
            groups.put(group1.name(), group1);
        }

        return true;
    }

    public void saveGroup(@NotNull TabGroups group) {
        final String name = groupsFiles.get(group);
        YamlConfigurations.save(
                new File("plugins/Velocitab").toPath().resolve(name + ".yml"),
                TabGroups.class,
                group,
                ConfigProvider.YAML_CONFIGURATION_PROPERTIES.header(TabGroups.CONFIG_HEADER).build()
        );
    }

    public Optional<Group> getGroupFromServer(@NotNull String server, @NotNull Velocitab plugin) {
        final List<Group> groups = new ArrayList<>(this.groups.values());
        final Optional<Group> defaultGroup = getGroup("default");
        if (defaultGroup.isEmpty()) {
            throw new IllegalStateException("No default tab group defined");
        }
        // Ensure the default group is always checked last
        groups.remove(defaultGroup.get());
        groups.add(defaultGroup.get());
        for (Group group : groups) {
            if (group.registeredServers(plugin, false)
                    .stream()
                    .anyMatch(s -> s.getServerInfo().getName().equalsIgnoreCase(server))) {
                return Optional.of(group);
            }
        }

        return Optional.empty();
    }

    public Optional<Group> getGroup(@NotNull String name) {
        return Optional.ofNullable(groups.get(name));
    }

    public int getGroupPosition(@NotNull Group group) {
        return groupsList.indexOf(group) + 1;
    }

    @NotNull
    public Collection<Group> getGroups() {
        return groups.values();
    }

    public Optional<List<RegisteredServer>> getCachedServers(@NotNull Group group) {
        return Optional.ofNullable(cachedServers.get(group));
    }

    public void cacheServers(@NotNull Group group, @NotNull List<RegisteredServer> servers) {
        cachedServers.put(group, servers);
    }

    @ApiStatus.Internal
    public Map<String, Group> getGroupsMap() {
        return groups;
    }

    @ApiStatus.Internal
    public Map<TabGroups, String> getGroupsFilesMap() {
        return groupsFiles;
    }

    public void loadGroupsBackup(@NotNull Map<String, Group> groups, @NotNull Map<TabGroups, String> groupsFiles) {
        this.groups.clear();
        this.groups.putAll(groups);
        this.groupsFiles.clear();
        this.groupsFiles.putAll(groupsFiles);
    }

}
