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

package net.william278.velocitab.player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Role implements Comparable<Role> {
    public static final int DEFAULT_WEIGHT = 0;
    public static final Role DEFAULT_ROLE = new Role(DEFAULT_WEIGHT, null, null, null);
    private final int weight;
    @Nullable
    private final String name;
    @Nullable
    private final String prefix;
    @Nullable
    private final String suffix;

    public Role(int weight, @Nullable String name, @Nullable String prefix, @Nullable String suffix) {
        this.weight = weight;
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public int compareTo(@NotNull Role o) {
        return weight - o.weight;
    }

    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix);
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @NotNull
    protected String getWeightString(int highestWeight) {
        return String.format("%0" + (highestWeight + "").length() + "d", highestWeight - weight);
    }
}
