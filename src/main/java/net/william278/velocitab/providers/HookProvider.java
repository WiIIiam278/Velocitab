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

    /**
     * Retrieves the list of hooks associated with the HookProvider.
     *
     * @return The list of hooks associated with the HookProvider.
     */
    List<Hook> getHooks();

    /**
     * Sets the list of hooks associated with the HookProvider.
     *
     * @param hooks The list of hooks to set.
     */
    void setHooks(List<Hook> hooks);

    /**
     * Retrieves the instance of the Velocitab plugin.
     *
     * @return The instance of the Velocitab plugin.
     */
    Velocitab getPlugin();

    /**
     * Loads the hooks associated with the HookProvider.
     */
    default void loadHooks() {
        List<Hook> hooks = new ArrayList<>();
        Hook.AVAILABLE.forEach(availableHook -> availableHook.apply(getPlugin()).ifPresent(hooks::add));
        setHooks(hooks);
    }

    /**
     * Retrieves a hook of the specified type from the list of hooks associated with the HookProvider.
     *
     * @param hookType The class object representing the type of the hook to retrieve.
     * @param <H>      The type of the hook to retrieve.
     * @return An Optional containing the hook of the specified type, or an empty Optional if the hook is not found.
     */
    private <H extends Hook> Optional<H> getHook(@NotNull Class<H> hookType) {
        return getHooks().stream()
                .filter(hook -> hook.getClass().equals(hookType))
                .map(hookType::cast)
                .findFirst();
    }

    /**
     * Retrieves the LuckPermsHook from the list of hooks associated with the HookProvider.
     *
     * @return An Optional containing the LuckPermsHook, or an empty Optional if it is not found.
     */
    default Optional<LuckPermsHook> getLuckPermsHook() {
        return getHook(LuckPermsHook.class);
    }

    /**
     * Retrieves the PAPIProxyBridgeHook from the list of hooks associated with the HookProvider.
     *
     * @return An Optional containing the PAPIProxyBridgeHook, or an empty Optional if it is not found.
     */
    default Optional<PAPIProxyBridgeHook> getPAPIProxyBridgeHook() {
        return getHook(PAPIProxyBridgeHook.class);
    }

    /**
     * Retrieves the MiniPlaceholdersHook from the list of hooks associated with the HookProvider.
     *
     * @return An Optional containing the MiniPlaceholdersHook, or an empty Optional if it is not found.
     */
    default Optional<MiniPlaceholdersHook> getMiniPlaceholdersHook() {
        return getHook(MiniPlaceholdersHook.class);
    }

}
