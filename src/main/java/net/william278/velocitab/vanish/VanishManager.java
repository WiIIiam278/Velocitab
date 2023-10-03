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

package net.william278.velocitab.vanish;

import net.william278.velocitab.Velocitab;

import java.util.Objects;

public class VanishManager {

    private final Velocitab plugin;
    private VanishIntegration integration;

    public VanishManager(Velocitab plugin) {
        this.plugin = plugin;
        setIntegration(null);
    }

    public void setIntegration(VanishIntegration integration) {
        this.integration = Objects.requireNonNullElseGet(integration, DefaultVanishIntegration::new);
    }

    public boolean canSee(String name, String otherName) {
        return integration.canSee(name, otherName);
    }

    public boolean isVanished(String name) {
        return integration.isVanished(name);
    }


    public void vanish(String name) {

    }

    public void unvanish(String name) {

    }
}
