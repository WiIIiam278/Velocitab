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


package net.william278.velocitab.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import lombok.Getter;
import net.william278.velocitab.packet.versions.AbstractVersion;
import net.william278.velocitab.packet.versions.CurrentVersion;
import net.william278.velocitab.packet.versions.Version1_12_2;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class VersionManager {

    @Getter
    private static VersionManager instance;
    private final Set<AbstractVersion> versions;

    public VersionManager() {
        instance = this;
        this.versions = new CopyOnWriteArraySet<>();
        registerVersions();
    }

    private void registerVersions() {
        versions.add(new CurrentVersion());
        versions.add(new Version1_12_2());
    }

    public AbstractVersion getVersion(ProtocolVersion protocolVersion) {
        return versions.stream()
                .filter(version -> version.getProtocolVersions().contains(protocolVersion))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No version found for protocol version " + protocolVersion));
    }

}
