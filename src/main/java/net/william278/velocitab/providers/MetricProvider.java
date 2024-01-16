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
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

public interface MetricProvider {

    int METRICS_ID = 18247;

    /**
     * Retrieves the Metrics Factory used by the MetricProvider.
     *
     * @return The Metrics Factory used by the MetricProvider.
     */
    Metrics.Factory getMetricsFactory();

    /**
     * Retrieves the Velocitab plugin instance.
     * @return
     */
    Velocitab getPlugin();

    /**
     * Registers metrics for the Velocitab plugin using the Metrics library.
     * This method adds custom charts to the metrics object, which include information such as:
     * - Whether player sorting is enabled or disabled
     * - The type of formatter being used
     * - Whether LuckPerms hook is present
     * - Whether PAPIProxyBridge hook is present
     * - Whether MiniPlaceholders hook is present
     */
    default void registerMetrics() {
        final Metrics metrics = getMetricsFactory().make(this, METRICS_ID);
        metrics.addCustomChart(new SimplePie("sort_players", () -> getPlugin().getSettings().isSortPlayers() ? "Enabled" : "Disabled"));
        metrics.addCustomChart(new SimplePie("formatter_type", () -> getPlugin().getFormatter().getName()));
        metrics.addCustomChart(new SimplePie("using_luckperms", () -> getPlugin().getLuckPermsHook().isPresent() ? "Yes" : "No"));
        metrics.addCustomChart(new SimplePie("using_papiproxybridge", () -> getPlugin().getPAPIProxyBridgeHook().isPresent() ? "Yes" : "No"));
        metrics.addCustomChart(new SimplePie("using_miniplaceholders", () -> getPlugin().getMiniPlaceholdersHook().isPresent() ? "Yes" : "No"));
    }

}
