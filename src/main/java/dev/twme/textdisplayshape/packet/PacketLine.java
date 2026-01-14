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
 * Line implementation using EntityLib packet-based display.
 * Only sends packets to specified viewer players, does not create actual
 * entities on the server.
 */
public class PacketLine implements Shape {

    private final Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final float thickness;
    private final float roll;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;

    private final List<WrapperEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private final Set<Player> viewers = new HashSet<>();
    private boolean spawned = false;

    private PacketLine(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.color = builder.color;
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
    }

    @Override
    public void spawn() {
        if (spawned)
            return;

        // Front face: p1 -> p2
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(p1, p2, thickness, roll);
        createWrapperEntity(matrix);

        // Back face: swap p1 and p2, and add PI to roll for opposite direction
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayLine(p2, p1, thickness, roll + (float) Math.PI);
            createWrapperEntity(backMatrix);
        }

        spawned = true;
    }

    private void createWrapperEntity(Matrix4f matrix) {
        // Adjust transformation matrix: convert absolute coordinates to relative to
        // spawn location
        Matrix4f adjustedMatrix = new Matrix4f()
                .translate(
                        (float) -origin.getX(),
                        (float) -origin.getY(),
                        (float) -origin.getZ())
                .mul(matrix);

        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(origin));

        if (entity.getEntityMeta() instanceof TextDisplayMeta meta) {
            // Set TextDisplay properties
            meta.setText(net.kyori.adventure.text.Component.text(" "));
            meta.setBackgroundColor(color.asARGB());
            meta.setSeeThrough(seeThrough);

            // Set brightness
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                displayMeta.setBrightnessOverride(blockLight << 4 | skyLight << 20);
            }

            // Set transformation matrix using scale, translation, rotation separately
            setTransformFromMatrix(entity, adjustedMatrix);
        }

        // Add all viewers
        for (UUID uuid : viewerUUIDs) {
            entity.addViewer(uuid);
        }

        entities.add(entity);
    }

    /**
     * Sets translation, scale, and rotation from a Matrix4f.
     */
    private void setTransformFromMatrix(WrapperEntity entity, Matrix4f matrix) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta meta))
            return;

        // Extract translation
        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);

        // Extract scale
        Vector3f scale = new Vector3f();
        matrix.getScale(scale);

        // Extract rotation
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
     * Builder class.
     */
    public static class Builder implements ShapeBuilder<PacketLine> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final float thickness;

        private float roll = 0f;
        private Color color = Color.fromARGB(200, 255, 100, 100);
        private boolean doubleSided = false;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;

        public Builder(Location origin, Vector3f p1, Vector3f p2, float thickness) {
            this.origin = origin;
            this.p1 = p1;
            this.p2 = p2;
            this.thickness = thickness;
        }

        /**
         * Sets the roll angle (rotation around the line axis).
         * This allows the line surface to face different directions.
         * Default is 0 (facing up when the line is horizontal).
         *
         * @param roll the roll angle in radians
         * @return this builder
         */
        public Builder roll(float roll) {
            this.roll = roll;
            return this;
        }

        /**
         * Sets the roll angle in degrees (rotation around the line axis).
         * This allows the line surface to face different directions.
         * Default is 0 (facing up when the line is horizontal).
         *
         * @param degrees the roll angle in degrees
         * @return this builder
         */
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
        public PacketLine build() {
            return new PacketLine(this);
        }
    }
}
