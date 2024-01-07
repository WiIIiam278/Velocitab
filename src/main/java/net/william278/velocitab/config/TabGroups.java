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

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.velocitab.tab.Nametag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TabGroups implements ConfigValidator {

    public static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃      Velocitab TabGroups     ┃
            ┃    Developed by William278   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/velocitab
            ┗╸ Documentation: https://william278.net/docs/velocitab""";

    public List<Group> groups = List.of(
            new Group(
                    "default",
                    List.of("&rainbow&Running Velocitab by William278"),
                    List.of("[There are currently %players_online%/%max_players_online% players online](gray)"),
                    "&7[%server%] &f%prefix%%username%",
                    new Nametag("&f%prefix%", "&f%suffix%"),
                    List.of("lobby", "survival", "creative", "minigames", "skyblock", "prison", "hub"),
                    List.of("%role_weight%", "%username%"),
                    1000,
                    1000
            )
    );

    @NotNull
    public Group getGroupFromName(@NotNull String name) {
        return groups.stream()
                .filter(group -> group.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No group with name " + name + " found"));
    }

    @NotNull
    public Group getGroupFromServer(@NotNull String server) {
        for (Group group : groups) {
            if (group.servers().contains(server)) {
                return group;
            }
        }
        return getGroupFromName("default");
    }

    public int getPosition(@NotNull Group group) {
        return groups.indexOf(group) + 1;
    }


    @Override
    public void validateConfig() {
        if (groups.isEmpty()) {
            throw new IllegalStateException("No tab groups defined in config");
        }
        if (groups.stream().noneMatch(group -> group.name().equals("default"))) {
            throw new IllegalStateException("No default tab group defined in config");
        }
    }
}
