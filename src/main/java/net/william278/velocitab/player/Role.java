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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Role implements Comparable<Role> {
    public static final int DEFAULT_WEIGHT = 0;
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

    public Role(int weight, @Nullable String name, @Nullable String displayName, @Nullable String prefix, @Nullable String suffix) {
        this.weight = weight;
        this.name = name;
        this.displayName = displayName;
        this.prefix = prefix;
        this.suffix = suffix;
    }

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
    protected String getWeightString() {
        return compressNumber(Integer.MAX_VALUE / 4d - weight);
    }

    public String compressNumber(double number) {
        int wholePart = (int) number;

        char decimalChar = (char) ((number - wholePart) * Character.MAX_VALUE);

        List<Character> charList = new ArrayList<>();

        while (wholePart > 0) {
            char digit = (char) (wholePart % Character.MAX_VALUE);

            charList.add(0, digit);

            wholePart /= Character.MAX_VALUE;
        }

        if (charList.isEmpty()) {
            charList.add((char) 0);
        }

        String charString = charList.stream().map(String::valueOf).collect(Collectors.joining());

        charString += decimalChar;

        return charString;
    }

}
