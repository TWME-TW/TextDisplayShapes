package dev.twme.textdisplayshape.packet;

import com.github.retrooper.packetevents.protocol.player.User;
import io.github.twme.virtualentities.VirtualEntities;
import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;
import io.github.twme.virtualentities.VirtualViewer;
import org.bukkit.Location;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

/**
 * Factory class for packet-based shapes.
 * Uses VirtualEntities and PacketEvents to display shapes to selected viewers.
 */
public class PacketShapeFactory implements AutoCloseable {
    private final VirtualEntityManager entityManager;
    private final boolean ownsEntityManager;

    /** Creates a factory with a manager owned by this factory. */
    public PacketShapeFactory() {
        this(VirtualEntities.create(), true);
    }

    /**
     * Creates a factory backed by an application-managed entity manager.
     * The caller remains responsible for closing the supplied manager.
     */
    public PacketShapeFactory(VirtualEntityManager entityManager) {
        this(entityManager, false);
    }

    private PacketShapeFactory(VirtualEntityManager entityManager, boolean ownsEntityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
        this.ownsEntityManager = ownsEntityManager;
    }

    public VirtualEntityManager entityManager() {
        return entityManager;
    }

    /**
     * Returns the canonical VirtualEntities viewer for this factory's manager and
     * PacketEvents connection. Reuse it for application-owned virtual entities.
     */
    public VirtualViewer viewer(User user) {
        return VirtualTextDisplaySupport.viewer(entityManager, Objects.requireNonNull(user, "user"));
    }

    /** Adds an application-owned virtual entity to the canonical connection viewer. */
    public VirtualEntity addViewer(VirtualEntity entity, User user) {
        entity.addViewer(viewer(user));
        return entity;
    }

    public PacketTriangle.Builder triangle(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new PacketTriangle.Builder(origin, p1, p2, p3).withEntityManager(entityManager);
    }

    public PacketLine.Builder line(Location origin, Vector3f p1, Vector3f p2, float thickness) {
        return new PacketLine.Builder(origin, p1, p2, thickness).withEntityManager(entityManager);
    }

    public PacketPolyline.Builder polyline(Location origin, List<Vector3f> points, float thickness) {
        return new PacketPolyline.Builder(origin, points, thickness).withEntityManager(entityManager);
    }

    public PacketParallelogram.Builder parallelogram(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new PacketParallelogram.Builder(origin, p1, p2, p3).withEntityManager(entityManager);
    }

    @Override
    public void close() {
        if (ownsEntityManager) {
            entityManager.close();
        }
    }
}
