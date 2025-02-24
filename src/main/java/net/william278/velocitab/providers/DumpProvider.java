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
import net.william278.toilet.DumpOptions;
import net.william278.toilet.Toilet;
import net.william278.toilet.dump.DumpUser;
import net.william278.toilet.dump.PluginStatus;
import net.william278.toilet.dump.ProjectMeta;
import net.william278.toilet.velocity.VelocityToilet;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.hook.Hook;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import static net.william278.toilet.DumpOptions.*;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface DumpProvider {

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
                ? new DumpUser(o.getUsername(), o.getUniqueId()) : null).toString();
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
                        .author("William278, AlexDev_")
                        .sourceCode("https://github.com/WiIIiam278/Velocitab")
                        .website("https://william278.net/project/velocitab")
                        .support("https://discord.gg/tVYhJfyDWG")
                        .build())
                .fileInclusionRules(getFileInclusionRules())
                .build();
    }

    @NotNull
    private List<FileInclusionRule> getFileInclusionRules() {
        final List<File> tabGroupsFiles = getPlugin().getTabGroupsManager().getGroupsFiles();
        final List<FileInclusionRule> rules = Lists.newArrayList();
        rules.add(FileInclusionRule.configFile("settings.yml", "Settings File"));
        for (File tabGroupsFile : tabGroupsFiles) {
            rules.add(FileInclusionRule.configFile(tabGroupsFile.getName(), "Tab Groups File"));
        }

        return rules;
    }

    @NotNull
    @Blocking
    private PluginStatus getPluginStatus() {
        return PluginStatus.builder()
                .blocks(List.of(getSystemStatus(), getHookStatus()))
                .build();
    }

    @NotNull
    @Blocking
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
                        Map.entry("RelationalPlaceholders", Boolean.toString(getPlugin().getSettings().isEnableRelationalPlaceholders()))
                ),
                "Plugin Status", "fa6-solid:wrench"
        );
    }

    @NotNull
    @Blocking
    private PluginStatus.ListStatusBlock getHookStatus() {
        return new PluginStatus.ListStatusBlock(
                getPlugin().getHooks().stream().map(Hook::getName).toList(),
                "Loaded Hooks", "fa6-solid:plug"
        );
    }

    @NotNull
    Velocitab getPlugin();
}