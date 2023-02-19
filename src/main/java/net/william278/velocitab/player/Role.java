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
        return o.weight - weight;
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
    public String getStringComparableWeight() {
        return String.format("%03d", weight);
    }
}
