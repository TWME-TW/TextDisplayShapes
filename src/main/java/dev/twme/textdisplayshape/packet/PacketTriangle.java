package dev.twme.textdisplayshape.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TRSResult;
import dev.twme.textdisplayshape.util.TextDisplayUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;

/**
 * Triangle implementation using EntityLib packet-based display.
 * Only sends packets to specified viewer players, does not create actual
 * entities on the server.
 */
public class PacketTriangle implements Shape {

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

    private PacketTriangle(Builder builder) {
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

        // Front face: p1, p2, p3 — use analytical TRS for precision
        for (TRSResult trs : TextDisplayUtil.computeTriangleTRS(p1, p2, p3)) {
            createWrapperEntityFromTRS(trs);
        }

        // Back face: swap p2 and p3
        if (doubleSided) {
            for (TRSResult trs : TextDisplayUtil.computeTriangleTRS(p1, p3, p2)) {
                createWrapperEntityFromTRS(trs);
            }
        }

        spawned = true;
    }

    private void createWrapperEntityFromTRS(TRSResult trs) {
        // Adjust translation: convert from absolute world coordinates to relative to spawn location
        Vector3f adjustedTranslation = new Vector3f(trs.translation())
                .sub((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());

        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(origin));

        if (entity.getEntityMeta() instanceof TextDisplayMeta meta) {
            // Set TextDisplay properties
            meta.setText(net.kyori.adventure.text.Component.text(" "));
            meta.setBackgroundColor(color.asARGB());
            meta.setSeeThrough(seeThrough);

            // Set brightness and transformation
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                displayMeta.setBrightnessOverride(blockLight << 4 | skyLight << 20);
                displayMeta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(
                        adjustedTranslation.x, adjustedTranslation.y, adjustedTranslation.z));
                displayMeta.setScale(new com.github.retrooper.packetevents.util.Vector3f(
                        trs.scale().x, trs.scale().y, trs.scale().z));
                displayMeta.setLeftRotation(new com.github.retrooper.packetevents.util.Quaternion4f(
                        trs.leftRotation().x, trs.leftRotation().y, trs.leftRotation().z, trs.leftRotation().w));
                displayMeta.setRightRotation(new com.github.retrooper.packetevents.util.Quaternion4f(
                        trs.rightRotation().x, trs.rightRotation().y, trs.rightRotation().z,
                        trs.rightRotation().w));
            }
        }

        // Add all viewers
        for (UUID uuid : viewerUUIDs) {
            entity.addViewer(uuid);
        }

        entities.add(entity);
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
    public static class Builder implements ShapeBuilder<PacketTriangle> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final Vector3f p3;

        private Color color = Color.fromARGB(150, 50, 100, 100);
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
        public PacketTriangle build() {
            return new PacketTriangle(this);
        }
    }
}
