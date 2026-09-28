package dev.twme.textdisplayshape.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;

import dev.twme.textdisplayshape.shape.BoxOutline;
import dev.twme.textdisplayshape.shape.ShapeStyle;
import io.github.twme.virtualentities.VirtualEntities;
import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;
import io.github.twme.virtualentities.VirtualViewer;
import io.github.twme.virtualentities.metadata.GeneratedEntityMetadataKeys;

class PacketShapeAnimationTest {

    private static final Location ORIGIN = new Location(null, 100, 64, 100);

    private VirtualEntityManager manager;
    private PacketShapeFactory factory;

    @BeforeAll
    static void initializePacketEvents() {
        PacketEventsAPI<?> api = mock(PacketEventsAPI.class);
        ServerManager serverManager = mock(ServerManager.class);
        when(api.getServerManager()).thenReturn(serverManager);
        when(api.getSettings()).thenReturn(new PacketEventsSettings());
        when(serverManager.getVersion()).thenReturn(ServerVersion.V_1_21_11);
        PacketEvents.setAPI(api);
    }

    @AfterAll
    static void clearPacketEvents() {
        PacketEvents.setAPI(null);
    }

    @BeforeEach
    void createManager() {
        manager = VirtualEntities.create();
        factory = new PacketShapeFactory(manager);
    }

    @AfterEach
    void closeManager() {
        factory.close();
        manager.close();
    }

    @Test
    void setPointsReusesEntitiesAndStartsInterpolation() {
        PacketLine line = factory.line(ORIGIN, new Vector3f(100, 64, 100), new Vector3f(102, 64, 100), 0.1f)
                .interpolationDuration(4)
                .build();
        line.spawn();
        VirtualEntity entity = line.getEntities().get(0);

        line.setPoints(new Vector3f(100, 65, 100), new Vector3f(103, 65, 100));

        assertSame(entity, line.getEntities().get(0), "updates must not respawn entities");
        assertEquals(4, entity.metadata()
                .get(GeneratedEntityMetadataKeys.Display.TRANSFORMATION_INTERPOLATION_DURATION).orElseThrow());
        assertEquals(0, entity.metadata()
                .get(GeneratedEntityMetadataKeys.Display.TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS)
                .orElseThrow());
        var translation = entity.metadata().get(GeneratedEntityMetadataKeys.Display.TRANSLATION).orElseThrow();
        assertEquals(1f, translation.getY(), 0.2f, "translation is relative to the origin");
        assertEquals(new Vector3f(103, 65, 100), line.getEnd());
    }

    @Test
    void updatesAreSentAsOneBundlePerViewer() {
        PacketTriangle triangle = factory.triangle(ORIGIN,
                        new Vector3f(100, 64, 100), new Vector3f(101, 64, 100), new Vector3f(100, 65, 100))
                .interpolationDuration(2)
                .build();
        triangle.spawn();
        List<PacketWrapper<?>> sent = new ArrayList<>();
        VirtualViewer viewer = VirtualViewer.of(UUID.randomUUID(), sent::add);
        for (VirtualEntity entity : triangle.getEntities()) {
            entity.addViewer(viewer);
        }
        sent.clear();

        triangle.setPoints(new Vector3f(100, 64, 100), new Vector3f(102, 64, 100), new Vector3f(100, 66, 100));

        long metadataPackets = sent.stream().filter(WrapperPlayServerEntityMetadata.class::isInstance).count();
        assertEquals(3, metadataPackets, "one metadata packet per triangle piece");
        assertTrue(sent.get(0).getClass().getSimpleName().contains("Bundle"), "bundle opens first: " + sent);
        assertTrue(sent.get(sent.size() - 1).getClass().getSimpleName().contains("Bundle"), "bundle closes last");
    }

    @Test
    void polylineGrowsAndShrinksWithItsPoints() {
        PacketPolyline polyline = factory.polyline(ORIGIN,
                List.of(new Vector3f(100, 64, 100), new Vector3f(101, 64, 100)), 0.1f).build();
        polyline.spawn();
        assertEquals(1, polyline.getEntityCount());

        polyline.setPoints(List.of(new Vector3f(100, 64, 100), new Vector3f(101, 64, 100),
                new Vector3f(101, 65, 100), new Vector3f(100, 65, 100)));
        assertEquals(3, polyline.getEntityCount());
        assertEquals(3, polyline.getSegmentCount());

        polyline.setPoints(List.of(new Vector3f(100, 64, 100), new Vector3f(101, 64, 100)));
        assertEquals(1, polyline.getEntityCount());
    }

    @Test
    void setColorFadesExistingEntities() {
        PacketParallelogram face = factory.parallelogram(ORIGIN,
                        new Vector3f(100, 64, 100), new Vector3f(101, 64, 100), new Vector3f(100, 65, 100))
                .doubleSided(true)
                .interpolationDuration(6)
                .build();
        face.spawn();
        face.setColor(0x4000FF00);

        assertEquals(0x4000FF00, face.getColor());
        for (VirtualEntity entity : face.getEntities()) {
            assertEquals(0x4000FF00, entity.metadata()
                    .get(GeneratedEntityMetadataKeys.TextDisplay.BACKGROUND_COLOR).orElseThrow());
            assertEquals(6, entity.metadata()
                    .get(GeneratedEntityMetadataKeys.Display.TRANSFORMATION_INTERPOLATION_DURATION).orElseThrow());
        }
    }

    @Test
    void translateMovesEntitiesWithTeleportDuration() {
        PacketLine line = factory.line(ORIGIN, new Vector3f(100, 64, 100), new Vector3f(101, 64, 100), 0.1f)
                .teleportDuration(3)
                .build();
        line.spawn();
        var before = line.getEntities().get(0).metadata().get(GeneratedEntityMetadataKeys.Display.TRANSLATION)
                .orElseThrow();

        line.translate(0, 2, 0);

        VirtualEntity entity = line.getEntities().get(0);
        assertEquals(66, entity.location().getY(), 1e-9);
        assertEquals(3, entity.metadata()
                .get(GeneratedEntityMetadataKeys.Display.POS_ROT_INTERPOLATION_DURATION).orElseThrow());
        var after = entity.metadata().get(GeneratedEntityMetadataKeys.Display.TRANSLATION).orElseThrow();
        assertEquals(before.getY(), after.getY(), 1e-6f, "relative geometry is unchanged");
        assertEquals(new Vector3f(100, 66, 100), line.getStart());
        assertEquals(66, line.getOrigin().getY(), 1e-9);
    }

    @Test
    void rootAnchoredTranslateOnlyMovesTheAnchor() {
        PacketLine line = factory.line(ORIGIN, new Vector3f(100, 64, 100), new Vector3f(101, 64, 100), 0.1f)
                .rootAnchor(true)
                .teleportDuration(2)
                .build();
        line.spawn();
        VirtualEntity anchor = line.getRootAnchor();
        assertNotNull(anchor);

        line.translate(1, 0, 0);

        assertEquals(101, anchor.location().getX(), 1e-9);
        assertEquals(100, line.getEntities().get(0).location().getX(), 1e-9, "passengers ride the anchor");
        line.remove();
        assertNull(line.getRootAnchor());
    }

    @Test
    void boxOutlineUsesTwelveAnimatedLines() {
        BoxOutline outline = factory.boxOutline(ORIGIN, new Vector3f(100, 64, 100), new Vector3f(101, 65, 101),
                0.05f, ShapeStyle.DEFAULT.withInterpolationDuration(3));
        outline.spawn();
        assertEquals(12, outline.getEntityCount());

        outline.setBounds(new Vector3f(100, 64, 100), new Vector3f(102, 66, 102));
        for (var member : outline.getMembers()) {
            VirtualEntity entity = ((PacketLine) member).getEntities().get(0);
            assertEquals(3, entity.metadata()
                    .get(GeneratedEntityMetadataKeys.Display.TRANSFORMATION_INTERPOLATION_DURATION).orElseThrow());
        }
        outline.remove();
        assertEquals(0, outline.getEntityCount());
    }

    @Test
    void invalidUpdatesLeaveTheShapeUnchanged() {
        PacketLine line = factory.line(ORIGIN, new Vector3f(100, 64, 100), new Vector3f(101, 64, 100), 0.1f).build();
        line.spawn();
        Vector3f same = new Vector3f(105, 64, 100);
        assertThrows(IllegalArgumentException.class, () -> line.setPoints(same, new Vector3f(same)));
        assertEquals(new Vector3f(101, 64, 100), line.getEnd());
        assertThrows(IllegalArgumentException.class, () -> line.setTeleportDuration(60));
    }
}
