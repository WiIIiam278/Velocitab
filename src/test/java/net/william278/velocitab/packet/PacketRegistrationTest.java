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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PacketRegistrationTest {
    private final List<Runnable> restore = new ArrayList<>();
    private final net.william278.velocitab.Velocitab plugin = new net.william278.velocitab.Velocitab(
            null, org.slf4j.helpers.NOPLogger.NOP_LOGGER, null);

    private static StateRegistry.PacketRegistry clientbound() throws ReflectiveOperationException {
        return (StateRegistry.PacketRegistry) field(StateRegistry.PLAY, StateRegistry.class, "clientbound");
    }

    private static Object field(Object target, Class<?> owner, String name) throws ReflectiveOperationException {
        final var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static StateRegistry.PacketRegistry.ProtocolRegistry registry(ProtocolVersion version) throws ReflectiveOperationException {
        final var method = StateRegistry.PacketRegistry.class.getDeclaredMethod("getProtocolRegistry", ProtocolVersion.class);
        method.setAccessible(true);
        return (StateRegistry.PacketRegistry.ProtocolRegistry) method.invoke(clientbound(), version);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void snapshotRegistry() throws ReflectiveOperationException {
        // Velocity's registry is global. Restore it even when a regression assertion fails.
        for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
            final var registry = registry(version);
            for (String name : List.of("packetClassToId", "packetIdToSupplier")) {
                final Map<Object, Object> live = (Map<Object, Object>) field(registry, registry.getClass(), name);
                final Map<Object, Object> copy = new HashMap<>(live);
                restore.add(() -> { live.clear(); live.putAll(copy); });
            }
        }
    }

    @AfterEach
    void restoreRegistry() {
        restore.forEach(Runnable::run);
    }

    @Test
    void coldStartRegistersAllSupportedTeamProtocols() throws ReflectiveOperationException {
        new ScoreboardManager(plugin, true).registerPacket();
        for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
            if (version.noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
                final var registry = registry(version);
                final var packet = new UpdateTeamsPacket(null);
                assertTrue(registry.containsPacket(packet), version.toString());
                assertInstanceOf(UpdateTeamsPacket.class, registry.createPacket(registry.getPacketId(packet)));
            }
        }
        assertEquals(0x6B, registry(ProtocolVersion.MINECRAFT_1_21_11).getPacketId(new UpdateTeamsPacket(null)));
    }

    @Test
    void unregisterRemovesEncoderAndDecoderAndAllowsReregistration() throws ReflectiveOperationException {
        final ScoreboardManager manager = new ScoreboardManager(plugin, true);
        for (int attempt = 0; attempt < 3; attempt++) {
            manager.registerPacket();
            final var registry = registry(ProtocolVersion.MINECRAFT_1_21_11);
            assertEquals(0x6B, registry.getPacketId(new UpdateTeamsPacket(null)));
            manager.unregisterPacket();
            assertFalse(registry.containsPacket(new UpdateTeamsPacket(null)), "encoder mapping left behind");
            assertNull(registry.createPacket(0x6B), "decoder mapping left behind");
        }
    }

    @Test
    void unregisterDoesNotInstantiateUnrelatedPacketSuppliers() {
        final var calls = new java.util.concurrent.atomic.AtomicInteger();
        final var unrelated = PacketRegistration.of(UnrelatedPacket.class)
                .packetSupplier(() -> { calls.incrementAndGet(); return new UnrelatedPacket(); })
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_8, false);
        unrelated.register();
        final ScoreboardManager manager = new ScoreboardManager(plugin, true);
        manager.registerPacket();
        manager.unregisterPacket();
        assertEquals(0, calls.get(), "must not instantiate unrelated packets");
    }

    @Test
    void failedRegistrationRollsBackEarlierProtocolsAndPreservesConflictingPacket() throws ReflectiveOperationException {
        final var blocker = PacketRegistration.of(UnrelatedPacket.class)
                .packetSupplier(UnrelatedPacket::new)
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_21_11, false);
        blocker.register();
        final var registration = PacketRegistration.of(UpdateTeamsPacket.class)
                .packetSupplier(() -> new UpdateTeamsPacket(null))
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_8, false);
        assertThrows(RuntimeException.class, registration::register);
        for (ProtocolVersion version : ProtocolVersion.SUPPORTED_VERSIONS) {
            assertFalse(registry(version).containsPacket(new UpdateTeamsPacket(null)), "partial mapping left for " + version);
        }
        assertInstanceOf(UnrelatedPacket.class, registry(ProtocolVersion.MINECRAFT_1_21_11).createPacket(0x7FFE));
        blocker.unregister();
        assertDoesNotThrow(registration::register);
    }

    @Test
    void scoreboardRegistrationFailureMustAbortInitialization() {
        final var blocker = PacketRegistration.of(UnrelatedPacket.class)
                .packetSupplier(UnrelatedPacket::new)
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x6B, ProtocolVersion.MINECRAFT_1_21_11, false);
        blocker.register();
        final ScoreboardManager manager = new ScoreboardManager(plugin, true);
        final var failure = assertThrows(IllegalStateException.class, manager::registerPacket);
        assertTrue(failure.getMessage().contains("UpdateTeamsPacket"));
        assertNotNull(failure.getCause());
    }

    @Test
    void duplicateRegistrationDoesNotRemoveExistingMappings() throws ReflectiveOperationException {
        final var registration = PacketRegistration.of(UpdateTeamsPacket.class)
                .packetSupplier(() -> new UpdateTeamsPacket(null))
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_8, false);
        registration.register();
        assertThrows(RuntimeException.class, registration::register);
        assertEquals(0x7FFE, registry(ProtocolVersion.MINECRAFT_1_21_11).getPacketId(new UpdateTeamsPacket(null)));
        assertInstanceOf(UpdateTeamsPacket.class, registry(ProtocolVersion.MINECRAFT_1_21_11).createPacket(0x7FFE));
    }

    @Test
    void failedContenderCannotUnregisterOwnersMappings() throws ReflectiveOperationException {
        final var owner = PacketRegistration.of(UpdateTeamsPacket.class)
                .packetSupplier(() -> new UpdateTeamsPacket(null))
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_8, false);
        final var contender = PacketRegistration.of(UpdateTeamsPacket.class)
                .packetSupplier(() -> new UpdateTeamsPacket(null))
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_8, false);

        owner.register();
        assertThrows(RuntimeException.class, contender::register);
        contender.unregister();

        final var registry = registry(ProtocolVersion.MINECRAFT_1_21_11);
        assertEquals(0x7FFE, registry.getPacketId(new UpdateTeamsPacket(null)));
        assertInstanceOf(UpdateTeamsPacket.class, registry.createPacket(0x7FFE));
        owner.unregister();
        assertFalse(registry.containsPacket(new UpdateTeamsPacket(null)));
        assertNull(registry.createPacket(0x7FFE));
    }

    @Test
    void encodeOnlyUnregisterRemovesEncoderWithoutCallingSupplier() throws ReflectiveOperationException {
        final var registration = PacketRegistration.of(UpdateTeamsPacket.class)
                .packetSupplier(() -> { throw new AssertionError("encode-only supplier must not be called"); })
                .direction(ProtocolUtils.Direction.CLIENTBOUND).stateRegistry(StateRegistry.PLAY)
                .mapping(0x7FFE, ProtocolVersion.MINECRAFT_1_8, true);
        registration.register();
        final var registry = registry(ProtocolVersion.MINECRAFT_1_21_11);
        assertEquals(0x7FFE, registry.getPacketId(new UpdateTeamsPacket(null)));
        assertNull(registry.createPacket(0x7FFE));
        registration.unregister();
        assertFalse(registry.containsPacket(new UpdateTeamsPacket(null)));
    }

    private static final class UnrelatedPacket extends UpdateTeamsPacket {
        UnrelatedPacket() { super(null); }
    }
}
