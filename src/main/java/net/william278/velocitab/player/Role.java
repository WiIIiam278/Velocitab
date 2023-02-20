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

    public String getStringComparableWeight(int maximumPossibleWeight, int lowestPossibleWeight) {
        // Calculate the weight range and the ratio of the input weight to the weight range
        int weightRange = maximumPossibleWeight - lowestPossibleWeight;
        double weightRatio = (double) (maximumPossibleWeight - weight) / weightRange;

        // Convert the weight ratio to a string with 3 decimal places and remove the decimal point
        String weightString = String.format("%.3f", weightRatio).replace(".", "");

        // Pad the weight string with leading zeros to a length of 6 characters
        weightString = String.format("%6s", weightString).replace(' ', '0');

        // Prepend a minus sign for negative weights
        if (weight < 0) {
            weightString = "-" + weightString.substring(1);
        } else {
            // Reverse the weight string for non-negative weights
            weightString = new StringBuilder(weightString).reverse().toString();
        }

        return weightString;
    }
}
