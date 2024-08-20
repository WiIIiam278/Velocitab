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

package net.william278.velocitab.config;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.desertwell.util.Version;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Configuration
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Metadata {

    private String velocityApiVersion;
    private int velocityMinimumBuild;
    private boolean ignoreBuildVersion;

    public void validateApiVersion(@NotNull Version version) {
        if (version.compareTo(Version.fromString(velocityApiVersion)) < 0) {
            final String serverVersion = version.toStringWithoutMetadata();
            throw new IllegalStateException("Your Velocity API version (" + serverVersion + ") is not supported! " +
                    "Disabling Velocitab. Please update to at least Velocity v" + velocityApiVersion
                    + " build #" + velocityMinimumBuild + " or newer.");
        }
    }

    public void validateBuild(@NotNull Version version) {
        if (ignoreBuildVersion) {
            return;
        }

        int serverBuild = getBuildNumber(version.toString());
        if (serverBuild < velocityMinimumBuild) {
            throw new IllegalStateException("Your Velocity build version (#" + serverBuild + ") is not supported! " +
                    "Disabling Velocitab. Please update to at least Velocity v" + velocityApiVersion
                    + " build #" + velocityMinimumBuild + " or newer.");
        }
    }

    private int getBuildNumber(@NotNull String proxyVersion) {
        final Matcher matcher = Pattern.compile(".*-b(\\d+).*").matcher(proxyVersion);
        if (matcher.find(1)) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("No build number found for proxy version: " + proxyVersion);
    }

}
