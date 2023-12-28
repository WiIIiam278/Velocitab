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

package net.william278.velocitab.util;

import net.william278.velocitab.Velocitab;
import net.william278.velocitab.hook.Hook;
import net.william278.velocitab.hook.LuckPermsHook;
import net.william278.velocitab.hook.MiniPlaceholdersHook;
import net.william278.velocitab.hook.PAPIProxyBridgeHook;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface HookProvider {

    List<Hook> getHooks();

    void setHooks(List<Hook> hooks);

    Velocitab getPlugin();

    default void loadHooks() {
        List<Hook> hooks = new ArrayList<>();
        Hook.AVAILABLE.forEach(availableHook -> availableHook.apply(getPlugin()).ifPresent(hooks::add));
        setHooks(hooks);
    }

    private <H extends Hook> Optional<H> getHook(@NotNull Class<H> hookType) {
        return getHooks().stream()
                .filter(hook -> hook.getClass().equals(hookType))
                .map(hookType::cast)
                .findFirst();
    }

    default Optional<LuckPermsHook> getLuckPermsHook() {
        return getHook(LuckPermsHook.class);
    }

    default Optional<PAPIProxyBridgeHook> getPAPIProxyBridgeHook() {
        return getHook(PAPIProxyBridgeHook.class);
    }

    default Optional<MiniPlaceholdersHook> getMiniPlaceholdersHook() {
        return getHook(MiniPlaceholdersHook.class);
    }

}
