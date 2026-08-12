package dev.twme.textdisplayshape.packet;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import io.github.twme.virtualentities.VirtualViewer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PacketViewerRegistryTest {
    @Test
    void reusesViewerForSamePacketEventsConnection() {
        PacketViewerRegistry registry = registry();
        UUID playerId = UUID.randomUUID();
        Object connection = new Object();

        VirtualViewer first = registry.viewer(playerId, connection, () -> viewer(playerId));
        VirtualViewer second = registry.viewer(playerId, connection, () -> viewer(playerId));

        assertSame(first, second);
    }

    @Test
    void replacesViewerWhenPlayerReconnects() {
        PacketViewerRegistry registry = registry();
        UUID playerId = UUID.randomUUID();
        VirtualViewer oldViewer = registry.viewer(playerId, new Object(), () -> viewer(playerId));
        VirtualViewer newViewer = registry.viewer(playerId, new Object(), () -> viewer(playerId));

        assertNotSame(oldViewer, newViewer);
    }

    private static PacketViewerRegistry registry() {
        return new PacketViewerRegistry();
    }

    private static VirtualViewer viewer(UUID playerId) {
        return new VirtualViewer() {
            @Override
            public UUID id() {
                return playerId;
            }

            @Override
            public void send(PacketWrapper<?> packet) {
            }
        };
    }
}
