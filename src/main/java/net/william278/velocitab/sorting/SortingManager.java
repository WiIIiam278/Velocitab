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

package net.william278.velocitab.sorting;

import com.google.common.base.Strings;
import net.william278.velocitab.Velocitab;
import net.william278.velocitab.config.Placeholder;
import net.william278.velocitab.player.TabPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class SortingManager {

    private final Velocitab plugin;
    private final int intSortSize = 3;

    public SortingManager(Velocitab plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<String> getTeamName(TabPlayer player) {
        return allOf(plugin.getSettings().getSortingElementList().stream()
                .map(e -> Placeholder.replace(e, plugin, player))
                .toList())
                .thenApply(v -> v.stream().map(this::adaptValue).toList())

                .thenApply(v -> String.join("-", v));
    }

    private String adaptValue(String value) {
        if (value.isEmpty()) {
            return "";
        }
        if (value.matches("[0-9]+")) {
            int weight = Integer.parseInt(value);
//            System.out.println(weight + " " + (weight >= 0 ? 0 : 1) + String.format("%0" + intSortSize + "d", Integer.parseInt(Strings.repeat("9", intSortSize)) - Math.abs(weight)));
            return (weight >= 0 ? 0 : 1) + String.format("%0" + intSortSize + "d", Integer.parseInt(Strings.repeat("9", intSortSize)) - Math.abs(weight));//
        }

        if (value.length() > 4) {
            return value.substring(0, 4);
        }

        return value;
    }

    private <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allFuturesResult =
                CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));
        return allFuturesResult.thenApply(v ->
                futuresList.stream().
                        map(CompletableFuture::join).
                        collect(Collectors.<T>toList())
        );
    }
}
