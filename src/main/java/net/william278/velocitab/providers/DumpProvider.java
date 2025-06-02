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

package net.william278.velocitab.providers;

import com.google.common.collect.Lists;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.william278.toilet.DumpOptions;
import net.william278.toilet.Toilet;
import net.william278.toilet.dump.*;
import net.william278.toilet.velocity.VelocityToilet;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Group;
import net.william278.velocitab.hook.Hook;
import net.william278.velocitab.util.DebugSystem;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import static net.william278.toilet.DumpOptions.*;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public interface DumpProvider {

    @NotNull PluginInfo.Label VANISH_INCOMPATIBLE = new PluginInfo.Label("Vanish Incompatible", "#bd3b01",
            "This plugin is not compatible with Velocitab as the developer has not added support for it. " +
                    "Vanished users will continue to appear in TAB.");

    @NotNull Map<String, PluginInfo.Label> INCOMPATIBLE_PLUGINS = Map.ofEntries(
            Map.entry("tab", PluginInfo.INCOMPATIBLE_LABEL)
    );

    @NotNull List<Integer> COLORS = Arrays.asList(
            Color.RED.getRGB(),
            Color.GREEN.getRGB(),
            Color.BLUE.getRGB(),
            Color.YELLOW.getRGB(),
            Color.ORANGE.getRGB(),
            Color.PINK.getRGB(),
            Color.CYAN.getRGB(),
            Color.MAGENTA.getRGB(),
            Color.GRAY.getRGB(),
            Color.LIGHT_GRAY.getRGB(),
            Color.DARK_GRAY.getRGB(),
            Color.BLACK.getRGB(),
            Color.WHITE.getRGB(),
            new Color(255, 165, 0).getRGB(), // Orange
            new Color(128, 0, 128).getRGB(), // Purple
            new Color(0, 128, 128).getRGB(), // Teal
            new Color(128, 128, 0).getRGB(), // Olive
            new Color(255, 192, 203).getRGB(), // Pink
            new Color(0, 255, 255).getRGB(), // Aqua
            new Color(255, 215, 0).getRGB()  // Gold
    );

    @NotNull String BYTEBIN_URL = "https://bytebin.lucko.me";
    @NotNull String VIEWER_URL = "https://william278.net/dump";

    @NotNull
    Toilet getToilet();

    void setToilet(@NotNull Toilet toilet);

    default void initializeToilet() {
        final Toilet toilet = VelocityToilet.create(getDumpOptions(), getPlugin().getServer());
        setToilet(toilet);
    }

    @NotNull
    @Blocking
    default String createDump(@NotNull CommandSource u) {
        return getToilet().dump(getPluginStatus(), u instanceof Player o
                ? new DumpUser(o.getUsername(), o.getUniqueId()) : null, getDebugLog()).toString();
    }

    @NotNull
    default DumpOptions getDumpOptions() {
        return builder()
                .bytebinUrl(BYTEBIN_URL)
                .viewerUrl(VIEWER_URL)
                .projectMeta(ProjectMeta.builder()
                        .id("velocitab")
                        .name("Velocitab")
                        .version(getPlugin().getVersion().toString())
                        .md5("unknown")
                        .author("William278, AlexDev03")
                        .sourceCode("https://github.com/WiIIiam278/Velocitab")
                        .website("https://william278.net/project/velocitab")
                        .support("https://discord.gg/tVYhJfyDWG")
                        .build())
                .compatibilityRules(getCompatibilityRules())
                .fileInclusionRules(getFileInclusionRules())
                .build();
    }

    @NotNull
    private ExtraFile getDebugLog() {
        return new ExtraFile("debug-log", "Internal Debugger", DebugSystem.getLogsAsString());
    }

    @NotNull
    private List<FileInclusionRule> getFileInclusionRules() {
        final List<File> tabGroupsFiles = getPlugin().getTabGroupsManager().getGroupsFiles();
        final List<FileInclusionRule> rules = Lists.newArrayList();
        rules.add(FileInclusionRule.configFile(getPlugin().getConfigDirectory().resolve("config.yml").toFile().getAbsolutePath(), "Config File"));

        for (File tabGroupsFile : tabGroupsFiles) {
            final boolean isDefault = getPlugin().getTabGroupsManager().isDefaultFile(tabGroupsFile);
            final String name = "Tab Groups File (" + (isDefault ? "default" : tabGroupsFile.getName()) + ")";
            final FileInclusionRule rule = FileInclusionRule.configFile(tabGroupsFile.getAbsolutePath(), name);
            if (isDefault) {
                rules.add(1, rule);
            } else {
                rules.add(rule);
            }
        }

        return rules;
    }

    @NotNull
    private List<CompatibilityRule> getCompatibilityRules() {
        return INCOMPATIBLE_PLUGINS.entrySet().stream()
                .filter(e -> getPlugin().getServer().getPluginManager().getPlugin(e.getKey()).isPresent())
                .map(e -> CompatibilityRule.builder()
                        .resourceName(e.getKey())
                        .labelToApply(e.getValue())
                        .build())
                .toList();
    }

    @NotNull
    @Blocking
    private PluginStatus getPluginStatus() {
        return PluginStatus.builder()
                .blocks(List.of(getSystemStatus(), getServersInEachGroup(), getPlayersInEachGroup(), getHookStatus()))
                .build();
    }

    @NotNull
    private PluginStatus.MapStatusBlock getSystemStatus() {
        return new PluginStatus.MapStatusBlock(
                Map.ofEntries(
                        Map.entry("RemoveNameTags", Boolean.toString(getPlugin().getSettings().isRemoveNametags())),
                        Map.entry("DisableHeaderFooterIfEmpty", Boolean.toString(getPlugin().getSettings().isDisableHeaderFooterIfEmpty())),
                        Map.entry("Formatter", getPlugin().getSettings().getFormatter().name()),
                        Map.entry("FallbackGroupEnabled", Boolean.toString(getPlugin().getSettings().isFallbackEnabled())),
                        Map.entry("FallbackGroup", getPlugin().getSettings().getFallbackGroup()),
                        Map.entry("PapiProxyBridge", Boolean.toString(getPlugin().getSettings().isEnablePapiHook())),
                        Map.entry("PapiCacheTime", Long.toString(getPlugin().getSettings().getPapiCacheTime())),
                        Map.entry("MiniPlaceholders", Boolean.toString(getPlugin().getSettings().isEnableMiniPlaceholdersHook())),
                        Map.entry("SendScoreboardPackets", Boolean.toString(getPlugin().getSettings().isSendScoreboardPackets())),
                        Map.entry("SortPlayers", Boolean.toString(getPlugin().getSettings().isSortPlayers())),
                        Map.entry("RelationalPlaceholders", Boolean.toString(getPlugin().getSettings().isEnableRelationalPlaceholders())),
                        Map.entry("VanishIntegration", getPlugin().getVanishManager().getIntegration().getClass().getName())
                ),
                "Plugin Status", "fa6-solid:wrench"
        );
    }

    @NotNull
    private PluginStatus.ListStatusBlock getHookStatus() {
        return new PluginStatus.ListStatusBlock(
                getPlugin().getHooks().stream().map(Hook::getName).toList(),
                "Loaded Hooks", "fa6-solid:plug"
        );
    }

    @NotNull
    private PluginStatus.ChartStatusBlock getPlayersInEachGroup() {
        final AtomicInteger colorIndex = new AtomicInteger(0);
        final Map<PluginStatus.ChartKey, Integer> players = getPlugin().getTabGroupsManager().getGroups().stream()
                .collect(Collectors.toMap(
                        g -> new PluginStatus.ChartKey(g.name(), "fa6-solid:server", COLORS.get(colorIndex.getAndIncrement() % COLORS.size())),
                        group -> group.getTabPlayers(getPlugin()).size()
                ));
        return new PluginStatus.ChartStatusBlock(
                players,
                PluginStatus.ChartType.PIE,
                "Online players per group",
                "fa6-solid:users"
        );
    }

    @NotNull
    private PluginStatus.MapStatusBlock getServersInEachGroup() {
        final Map<String, String> servers = getPlugin().getTabGroupsManager().getGroups().stream()
                .sorted(getGroupComparator(getPlugin()).reversed())
                .collect(Collectors.toMap(
                        Group::name,
                        g -> g.registeredServers(getPlugin(), false).stream()
                                .map(RegisteredServer::getServerInfo)
                                .map(ServerInfo::getName)
                                .collect(Collectors.joining(", ")),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
        return new PluginStatus.MapStatusBlock(
                servers,
                "Servers in each group",
                "fa6-solid:network-wired"
        );
    }

    default Comparator<Group> getGroupComparator(@NotNull Velocitab plugin) {
        return (g1, g2) -> {
            final int servers1 = g1.registeredServers(plugin, false).size();
            final int servers2 = g2.registeredServers(plugin, false).size();
            if (servers1 != servers2) {
                return servers1 - servers2;
            }
            return g1.servers().size() - g2.servers().size();
        };
    }

    @NotNull
    Velocitab getPlugin();
}