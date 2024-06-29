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
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.function.Supplier;

// Based on VPacketEvents PacketRegistration API
public final class PacketRegistration<P extends MinecraftPacket> {

    private final Class<P> packetClass;
    private Supplier<P> packetSupplier;
    private ProtocolUtils.Direction direction;
    private StateRegistry stateRegistry;
    private final List<StateRegistry.PacketMapping> mappings = new ArrayList<>();

    public PacketRegistration<P> packetSupplier(final @NotNull Supplier<P> packetSupplier) {
        this.packetSupplier = packetSupplier;
        return this;
    }

    public PacketRegistration<P> direction(final ProtocolUtils.Direction direction) {
        this.direction = direction;
        return this;
    }

    public PacketRegistration<P> stateRegistry(final @NotNull StateRegistry stateRegistry) {
        this.stateRegistry = stateRegistry;
        return this;
    }

    public PacketRegistration<P> mapping(
            final int id,
            final ProtocolVersion version,
            final boolean encodeOnly
    ) {
        try {
            final StateRegistry.PacketMapping mapping = (StateRegistry.PacketMapping) PACKET_MAPPING$map.invoke(
                    id, version, encodeOnly);
            this.mappings.add(mapping);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        return this;
    }

    public void register() {
        try {
            final StateRegistry.PacketRegistry packetRegistry = direction == ProtocolUtils.Direction.CLIENTBOUND
                    ? (StateRegistry.PacketRegistry) STATE_REGISTRY$clientBound.invoke(stateRegistry)
                    : (StateRegistry.PacketRegistry) STATE_REGISTRY$serverBound.invoke(stateRegistry);

            PACKET_REGISTRY$register.invoke(
                    packetRegistry,
                    packetClass,
                    packetSupplier,
                    mappings.toArray(StateRegistry.PacketMapping[]::new)
            );

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @SuppressWarnings("unchecked")
    public void unregister() {
        try {
            final StateRegistry.PacketRegistry packetRegistry = direction == ProtocolUtils.Direction.CLIENTBOUND
                    ? (StateRegistry.PacketRegistry) STATE_REGISTRY$clientBound.invoke(stateRegistry)
                    : (StateRegistry.PacketRegistry) STATE_REGISTRY$serverBound.invoke(stateRegistry);

            Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry> versions = (Map<ProtocolVersion, StateRegistry.PacketRegistry.ProtocolRegistry>) PACKET_REGISTRY$versions.invoke(packetRegistry);
            versions.forEach((protocolVersion, protocolRegistry) -> {
                try {
                    IntObjectMap<Supplier<?>> packetIdToSupplier = (IntObjectMap<Supplier<?>>) PACKET_REGISTRY$packetIdToSupplier.invoke(protocolRegistry);
                    Object2IntMap<Class<?>> packetClassToId = (Object2IntMap<Class<?>>) PACKET_REGISTRY$packetClassToId.invoke(protocolRegistry);
                    Set.copyOf(packetIdToSupplier.keySet()).stream()
                            .filter(supplier -> packetIdToSupplier.get(supplier).get().getClass().equals(packetClass))
                            .forEach(packetIdToSupplier::remove);
                    packetClassToId.values().intStream()
                            .filter(id -> Objects.equals(packetClassToId.getInt(packetClass), id))
                            .forEach(packetClassToId::removeInt);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            });

        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static <P extends MinecraftPacket> PacketRegistration<P> of(Class<P> packetClass) {
        return new PacketRegistration<>(packetClass);
    }

    private PacketRegistration(final @NotNull Class<P> packetClass) {
        this.packetClass = packetClass;
    }

    private static final MethodHandle STATE_REGISTRY$clientBound;
    private static final MethodHandle STATE_REGISTRY$serverBound;
    private static final MethodHandle PACKET_REGISTRY$register;
    private static final MethodHandle PACKET_REGISTRY$packetIdToSupplier;
    private static final MethodHandle PACKET_REGISTRY$packetClassToId;
    private static final MethodHandle PACKET_REGISTRY$versions;
    private static final MethodHandle PACKET_MAPPING$map;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            final MethodHandles.Lookup stateRegistryLookup = MethodHandles.privateLookupIn(StateRegistry.class, lookup);
            STATE_REGISTRY$clientBound = stateRegistryLookup.findGetter(StateRegistry.class, "clientbound", StateRegistry.PacketRegistry.class);
            STATE_REGISTRY$serverBound = stateRegistryLookup.findGetter(StateRegistry.class, "serverbound", StateRegistry.PacketRegistry.class);

            final MethodType mapType = MethodType.methodType(StateRegistry.PacketMapping.class, Integer.TYPE, ProtocolVersion.class, Boolean.TYPE);
            PACKET_MAPPING$map = stateRegistryLookup.findStatic(StateRegistry.class, "map", mapType);

            final MethodHandles.Lookup packetRegistryLookup = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.class, lookup);
            final MethodType registerType = MethodType.methodType(void.class, Class.class, Supplier.class, StateRegistry.PacketMapping[].class);
            PACKET_REGISTRY$register = packetRegistryLookup.findVirtual(StateRegistry.PacketRegistry.class, "register", registerType);
            PACKET_REGISTRY$versions = packetRegistryLookup.findGetter(StateRegistry.PacketRegistry.class, "versions", Map.class);

            final MethodHandles.Lookup protocolRegistryLookup = MethodHandles.privateLookupIn(StateRegistry.PacketRegistry.ProtocolRegistry.class, lookup);
            PACKET_REGISTRY$packetIdToSupplier = protocolRegistryLookup.findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetIdToSupplier", IntObjectMap.class);
            PACKET_REGISTRY$packetClassToId = protocolRegistryLookup.findGetter(StateRegistry.PacketRegistry.ProtocolRegistry.class, "packetClassToId", Object2IntMap.class);


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
