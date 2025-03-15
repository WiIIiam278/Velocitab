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

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.placeholder.PlaceholderReplacement;
import net.william278.velocitab.tab.Nametag;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TabGroups implements ConfigValidator {

    public static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃     Velocitab Tab Groups     ┃
            ┃  by William278 & AlexDev03   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/velocitab
            ┗╸ Documentation: https://william278.net/docs/velocitab""";

    private static final Group DEFAULT_GROUP = new Group(
            "default",
            List.of("<rainbow:!2>Running Velocitab by William278 & AlexDev03</rainbow>"),
            List.of("<gray>There are currently %players_online%/%max_players_online% players online</gray>"),
            "<gray>[%server%] %prefix%%username%</gray>",
            new Nametag("", ""),
            Set.of("lobby", "survival", "creative", "minigames", "skyblock", "prison", "hub"),
            List.of("%role_weight%", "%username_lower%"),
            Map.of(),
            false,
            1000,
            1000,
            1000,
            1000,
            false
    );

    public List<Group> groups = List.of(DEFAULT_GROUP);

    @NotNull
    @SuppressWarnings("unused")
    public Group getGroupFromName(@NotNull String name) {
        return groups.stream()
                .filter(group -> group.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No group with name %s found".formatted(name)));
    }

    public Optional<Group> getGroup(@NotNull String name) {
        return groups.stream()
                .filter(group -> group.name().equals(name))
                .findFirst();
    }

    @Override
    public void validateConfig(@NotNull Velocitab plugin, @NotNull String name) {
        if(name.equals("tab_groups")) {
            if (groups.isEmpty()) {
                throw new IllegalStateException("No tab groups defined in config " + name);
            }
            if (groups.stream().noneMatch(group -> group.name().equals("default"))) {
                throw new IllegalStateException("No default tab group defined in config " + name);
            }
        }

        final Multimap<Group, String> missingKeys = getMissingKeys();
        if (missingKeys.isEmpty()) {
            return;
        }

        fixMissingKeys(plugin, missingKeys, name);
    }

    @NotNull
    private Multimap<Group, String> getMissingKeys() {
        final Multimap<Group, String> missingKeys = Multimaps.newSetMultimap(Maps.newHashMap(), HashSet::new);

        for (Group group : groups) {
            if (group.format() == null) {
                missingKeys.put(group, "format");
            }
            if (group.nametag() == null) {
                missingKeys.put(group, "nametag");
            }
            if (group.servers() == null) {
                missingKeys.put(group, "servers");
            }
            if (group.sortingPlaceholders() == null) {
                missingKeys.put(group, "sortingPlaceholders");
            }

            if (group.placeholderReplacements() == null) {
                missingKeys.put(group, "placeholderReplacements");
            }

            if (group.headerFooterUpdateRate() == 0) {
                missingKeys.put(group, "headerFooterUpdateRate");
            }

            if (group.formatUpdateRate() == 0) {
                missingKeys.put(group, "formatUpdateRate");
            }

            if (group.nametagUpdateRate() == 0) {
                missingKeys.put(group, "nametagUpdateRate");
            }

            if (group.placeholderUpdateRate() == 0) {
                missingKeys.put(group, "placeholderUpdateRate");
            }
        }

        return missingKeys;
    }

    private void fixMissingKeys(@NotNull Velocitab plugin, @NotNull Multimap<Group, String> missingKeys, @NotNull String name) {
        missingKeys.forEach((group, keys) -> {
            plugin.log("Missing required key(s) " + keys + " for group " + group.name());
            plugin.log("Using default values for group " + group.name());

            groups.remove(group);

            group = new Group(
                    group.name(),
                    group.headers(),
                    group.footers(),
                    group.format() == null ? DEFAULT_GROUP.format() : group.format(),
                    group.nametag() == null ? DEFAULT_GROUP.nametag() : group.nametag(),
                    group.servers() == null ? DEFAULT_GROUP.servers() : group.servers(),
                    group.sortingPlaceholders() == null ? DEFAULT_GROUP.sortingPlaceholders() : group.sortingPlaceholders(),
                    group.placeholderReplacements() == null ? DEFAULT_GROUP.placeholderReplacements() : group.placeholderReplacements(),
                    group.collisions(),
                    group.headerFooterUpdateRate() == 0 ? DEFAULT_GROUP.headerFooterUpdateRate() : group.headerFooterUpdateRate(),
                    group.formatUpdateRate() == 0 ? DEFAULT_GROUP.formatUpdateRate() : group.formatUpdateRate(),
                    group.nametagUpdateRate() == 0 ? DEFAULT_GROUP.nametagUpdateRate() : group.nametagUpdateRate(),
                    group.placeholderUpdateRate() == 0 ? DEFAULT_GROUP.placeholderUpdateRate() : group.placeholderUpdateRate(),
                    group.onlyListPlayersInSameServer()
            );

            groups.add(group);
        });

        plugin.getTabGroupsManager().saveGroup(this);
    }
}
