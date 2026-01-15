package dev.twme.textdisplayshape.packet;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TextDisplayUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Polyline (connected line segments) implementation using EntityLib packets.
 * Creates multiple connected line segments from a list of points.
 */
public class PacketPolyline implements Shape {

    private final Location origin;
    private final List<Vector3f> points;
    private final float thickness;
    private final float roll;
    private final Color color;
    private final boolean doubleSided;
    private final boolean closed;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;

    private final List<WrapperEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private final Set<Player> viewers = new HashSet<>();
    private boolean spawned = false;

    private PacketPolyline(Builder builder) {
        this.origin = builder.origin;
        this.points = new ArrayList<>(builder.points);
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.color = builder.color;
        this.doubleSided = builder.doubleSided;
        this.closed = builder.closed;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
    }

    @Override
    public void spawn() {
        if (spawned)
            return;

        if (points.size() < 2) {
            spawned = true;
            return;
        }

        // Create line segments between consecutive points
        for (int i = 0; i < points.size() - 1; i++) {
            Vector3f p1 = points.get(i);
            Vector3f p2 = points.get(i + 1);
            spawnLineSegment(p1, p2);
        }

        // If closed, connect last point to first point
        if (closed && points.size() > 2) {
            Vector3f pLast = points.get(points.size() - 1);
            Vector3f pFirst = points.get(0);
            spawnLineSegment(pLast, pFirst);
        }

        spawned = true;
    }

    private void spawnLineSegment(Vector3f p1, Vector3f p2) {
        // Front face
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(p1, p2, thickness, roll);
        createWrapperEntity(matrix);

        // Back face
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayLine(p2, p1, thickness, roll + (float) Math.PI);
            createWrapperEntity(backMatrix);
        }
    }

    private void createWrapperEntity(Matrix4f matrix) {
        Matrix4f adjustedMatrix = new Matrix4f()
                .translate(
                        (float) -origin.getX(),
                        (float) -origin.getY(),
                        (float) -origin.getZ())
                .mul(matrix);

        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(origin));

        if (entity.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setText(net.kyori.adventure.text.Component.text(" "));
            meta.setBackgroundColor(color.asARGB());
            meta.setSeeThrough(seeThrough);

            if (entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                displayMeta.setBrightnessOverride(blockLight << 4 | skyLight << 20);
            }

            setTransformFromMatrix(entity, adjustedMatrix);
        }

        for (UUID uuid : viewerUUIDs) {
            entity.addViewer(uuid);
        }

        entities.add(entity);
    }

    private void setTransformFromMatrix(WrapperEntity entity, Matrix4f matrix) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta meta))
            return;

        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);

        Vector3f scale = new Vector3f();
        matrix.getScale(scale);

        org.joml.Quaternionf rotation = new org.joml.Quaternionf();
        matrix.getUnnormalizedRotation(rotation);

        meta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(
                translation.x, translation.y, translation.z));
        meta.setScale(new com.github.retrooper.packetevents.util.Vector3f(
                scale.x, scale.y, scale.z));
        meta.setLeftRotation(new com.github.retrooper.packetevents.util.Quaternion4f(
                rotation.x, rotation.y, rotation.z, rotation.w));
    }

    @Override
    public void remove() {
        for (WrapperEntity entity : entities) {
            entity.remove();
        }
        entities.clear();
        spawned = false;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public void addViewer(Player player) {
        viewers.add(player);
        viewerUUIDs.add(player.getUniqueId());
        if (spawned) {
            for (WrapperEntity entity : entities) {
                entity.addViewer(player.getUniqueId());
            }
        }
    }

    @Override
    public void removeViewer(Player player) {
        viewers.remove(player);
        viewerUUIDs.remove(player.getUniqueId());
        if (spawned) {
            for (WrapperEntity entity : entities) {
                entity.removeViewer(player.getUniqueId());
            }
        }
    }

    @Override
    public Set<Player> getViewers() {
        return new HashSet<>(viewers);
    }

    @Override
    public List<UUID> getEntityUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (WrapperEntity entity : entities) {
            uuids.add(entity.getUuid());
        }
        return uuids;
    }

    /**
     * Gets all WrapperEntity instances of this shape.
     *
     * @return list of WrapperEntity instances
     */
    public List<WrapperEntity> getEntities() {
        return new ArrayList<>(entities);
    }

    /**
     * Gets the number of line segments.
     *
     * @return number of line segments
     */
    public int getSegmentCount() {
        if (points.size() < 2)
            return 0;
        return closed ? points.size() : points.size() - 1;
    }

    /**
     * Builder class.
     */
    public static class Builder implements ShapeBuilder<PacketPolyline> {
        private final Location origin;
        private final List<Vector3f> points;
        private final float thickness;

        private float roll = 0f;
        private Color color = Color.fromARGB(200, 255, 100, 100);
        private boolean doubleSided = false;
        private boolean closed = false;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;

        public Builder(Location origin, List<Vector3f> points, float thickness) {
            this.origin = origin;
            this.points = new ArrayList<>(points);
            this.thickness = thickness;
        }

        /**
         * Sets whether the polyline should be closed.
         *
         * @param closed true to close the polyline
         * @return this builder
         */
        public Builder closed(boolean closed) {
            this.closed = closed;
            return this;
        }

        public Builder roll(float roll) {
            this.roll = roll;
            return this;
        }

        public Builder rollDegrees(float degrees) {
            this.roll = (float) Math.toRadians(degrees);
            return this;
        }

        @Override
        public Builder color(Color color) {
            this.color = color;
            return this;
        }

        @Override
        public Builder doubleSided(boolean doubleSided) {
            this.doubleSided = doubleSided;
            return this;
        }

        @Override
        public Builder brightness(int block, int sky) {
            this.blockLight = block;
            this.skyLight = sky;
            return this;
        }

        @Override
        public Builder seeThrough(boolean seeThrough) {
            this.seeThrough = seeThrough;
            return this;
        }

        @Override
        public PacketPolyline build() {
            return new PacketPolyline(this);
        }
    }
}
