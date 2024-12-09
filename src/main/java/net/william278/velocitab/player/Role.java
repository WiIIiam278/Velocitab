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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public class Role implements Comparable<Role> {

    public static final int DEFAULT_WEIGHT = -1;
    public static final Role DEFAULT_ROLE = new Role(DEFAULT_WEIGHT, null, null, null, null);
    @Getter
    private final int weight;
    @Nullable
    private final String name;
    @Nullable
    private final String displayName;
    @Nullable
    private final String prefix;
    @Nullable
    private final String suffix;

    @Override
    public int compareTo(@NotNull Role o) {
        return Double.compare(weight, o.weight);
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName).or(this::getName);
    }

    public Optional<String> getPrefix() {
        return Optional.ofNullable(prefix);
    }

    public Optional<String> getSuffix() {
        return Optional.ofNullable(suffix);
    }

    @NotNull
    protected Optional<String> getWeightString() {
        if (weight == -1) {
            return Optional.empty();
        }
        return Optional.of(Integer.toString(weight));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Role role = (Role) obj;
        return weight == role.weight &&
                Objects.equals(name, role.name) &&
                Objects.equals(displayName, role.displayName) &&
                Objects.equals(prefix, role.prefix) &&
                Objects.equals(suffix, role.suffix);
    }
}
