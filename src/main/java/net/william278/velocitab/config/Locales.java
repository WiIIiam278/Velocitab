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
import lombok.Getter;

@Configuration
@Getter
public class Locales {

    public static final String LOCALES_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃       Velocitab Locales      ┃
            ┃         Developed by         ┃
            ┃   William278 & AlexDev03     ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Information: https://william278.net/project/velocitab
            ┗╸ Documentation: https://william278.net/docs/velocitab""";

    private String systemDumpConfirm = """
            <color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| Prepare a system dump? This will include:</color>
            <gray>• Your latest server logs and Velocitab config files</gray>
            <gray>• Current plugin system status information</gray>
            <gray>• Information about your Java & Minecraft server environment</gray>
            <gray>• A list of other currently installed plugins</gray>
            <click:run_command:/velocitab dump confirm><hover:show_text:'<gray>Click to prepare dump'><color:#00fb9a>To confirm click here or use: <italic>/velocitab dump confirm</italic></color></click>
            """;
    private String systemDumpStarted = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| Preparing system status dump, please wait…</color>";
    private String systemDumpReady = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| System status dump prepared! Click here to view</color>";
    private String systemDumpReadyConsole = "<color:#00fb9a><bold>Velocitab</bold></color> <color:#00fb9a>| System status dump prepared! Url: %url%</color>";
}
