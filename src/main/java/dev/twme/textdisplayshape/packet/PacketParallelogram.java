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
 * Parallelogram implementation using EntityLib packet-based display.
 * Only sends packets to specified viewer players, does not create actual
 * entities on the server.
 */
public class PacketParallelogram implements Shape {

    private final Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final Vector3f p3;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;

    private final List<WrapperEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private final Set<Player> viewers = new HashSet<>();
    private boolean spawned = false;

    private PacketParallelogram(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.p3 = builder.p3;
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

        // Front face: p1, p2, p3
        Matrix4f matrix = TextDisplayUtil.textDisplayParallelogram(p1, p2, p3);
        createWrapperEntity(matrix);

        // Back face: swap p2 and p3
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayParallelogram(p1, p3, p2);
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
    /**
     * Sets translation, scale, and rotation from a Matrix4f.
     */
    private void setTransformFromMatrix(WrapperEntity entity, Matrix4f matrix) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta meta))
            return;

        dev.twme.textdisplayshape.util.TRSResult result = TextDisplayUtil.decompose(matrix);

        meta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(
                result.translation().x, result.translation().y, result.translation().z));
        meta.setScale(new com.github.retrooper.packetevents.util.Vector3f(
                result.scale().x, result.scale().y, result.scale().z));
        meta.setLeftRotation(new com.github.retrooper.packetevents.util.Quaternion4f(
                result.leftRotation().x, result.leftRotation().y, result.leftRotation().z, result.leftRotation().w));
        meta.setRightRotation(new com.github.retrooper.packetevents.util.Quaternion4f(
                result.rightRotation().x, result.rightRotation().y, result.rightRotation().z,
                result.rightRotation().w));
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
    public static class Builder implements ShapeBuilder<PacketParallelogram> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final Vector3f p3;

        private Color color = Color.fromARGB(150, 100, 50, 150);
        private boolean doubleSided = false;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;

        public Builder(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
            this.origin = origin;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
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
        public PacketParallelogram build() {
            return new PacketParallelogram(this);
        }
    }
}
