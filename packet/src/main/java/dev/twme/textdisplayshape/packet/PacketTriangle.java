package dev.twme.textdisplayshape.packet;

import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TRSResult;
import dev.twme.textdisplayshape.util.TextDisplayUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Triangle implementation using VirtualEntities and PacketEvents. */
public class PacketTriangle implements Shape {
    private Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final Vector3f p3;
    private final int argbColor;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;
    private final float viewRange;
    private final boolean rootAnchorEnabled;
    private final VirtualEntityManager entityManager;

    private final List<VirtualEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private VirtualEntity rootAnchor;
    private boolean spawned;

    private PacketTriangle(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.p3 = builder.p3;
        this.argbColor = builder.argbColor;
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
        this.rootAnchorEnabled = builder.rootAnchorEnabled;
        this.entityManager = builder.entityManager;
    }

    @Override
    public void spawn() {
        if (spawned) {
            return;
        }
        if (rootAnchorEnabled) {
            rootAnchor = PacketRootAnchorSupport.createRootAnchor(entityManager, origin, viewRange, Set.of());
        }
        for (TRSResult transform : TextDisplayUtil.computeTriangleTRS(p1, p2, p3)) {
            createVirtualEntity(transform);
        }
        if (doubleSided) {
            for (TRSResult transform : TextDisplayUtil.computeTriangleTRS(p1, p3, p2)) {
                createVirtualEntity(transform);
            }
        }
        if (rootAnchor != null) {
            VirtualTextDisplaySupport.addViewers(entityManager, rootAnchor, viewerUUIDs);
        }
        spawned = true;
    }

    private void createVirtualEntity(TRSResult transform) {
        Vector3f adjustedTranslation = new Vector3f(transform.translation())
                .sub((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());
        VirtualEntity entity = VirtualTextDisplaySupport.createTextDisplay(
                entityManager,
                origin,
                viewerUUIDs,
                virtualEntity -> {
                    VirtualTextDisplaySupport.configureTextDisplay(
                            virtualEntity,
                            argbColor,
                            seeThrough,
                            blockLight,
                            skyLight,
                            viewRange
                    );
                    VirtualTextDisplaySupport.setTransform(
                            virtualEntity,
                            adjustedTranslation,
                            transform.scale(),
                            transform.leftRotation(),
                            transform.rightRotation()
                    );
                }
        );
        entities.add(entity);
        if (rootAnchorEnabled) {
            PacketRootAnchorSupport.attachPassenger(rootAnchor, entity);
        }
    }

    @Override
    public void remove() {
        for (VirtualEntity entity : entities) {
            entity.remove();
        }
        entities.clear();
        if (rootAnchor != null) {
            rootAnchor.remove();
            rootAnchor = null;
        }
        spawned = false;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public void addViewer(UUID playerUUID) {
        viewerUUIDs.add(playerUUID);
        if (spawned) {
            for (VirtualEntity entity : entities) {
                VirtualTextDisplaySupport.addViewer(entityManager, entity, playerUUID);
            }
            if (rootAnchor != null) {
                VirtualTextDisplaySupport.addViewer(entityManager, rootAnchor, playerUUID);
            }
        }
    }

    @Override
    public void removeViewer(UUID playerUUID) {
        viewerUUIDs.remove(playerUUID);
        if (spawned) {
            if (rootAnchor != null) {
                rootAnchor.removeViewer(playerUUID);
            }
            for (VirtualEntity entity : entities) {
                entity.removeViewer(playerUUID);
            }
        }
    }

    @Override
    public Set<UUID> getViewerUUIDs() {
        return new HashSet<>(viewerUUIDs);
    }

    @Override
    public List<UUID> getEntityUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (VirtualEntity entity : entities) {
            uuids.add(entity.uuid());
        }
        return uuids;
    }

    public List<VirtualEntity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public void teleportOrigin(double x, double y, double z) {
        if (!spawned) {
            return;
        }
        Location newOrigin = new Location(origin.getWorld(), x, y, z);
        if (rootAnchorEnabled && rootAnchor != null) {
            PacketRootAnchorSupport.teleportRootAnchor(entityManager, rootAnchor, entities, origin, newOrigin);
        } else {
            VirtualTextDisplaySupport.rebaseEntities(entityManager, entities, origin, newOrigin);
        }
        origin = newOrigin.clone();
    }

    public static class Builder implements ShapeBuilder<PacketTriangle> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final Vector3f p3;
        private int argbColor = Color.fromARGB(150, 50, 100, 100).asARGB();
        private boolean doubleSided;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;
        private boolean rootAnchorEnabled;
        private VirtualEntityManager entityManager = PacketEntityManagers.defaultManager();

        public Builder(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
            this.origin = origin;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        Builder withEntityManager(VirtualEntityManager entityManager) {
            this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
            return this;
        }

        public Builder color(Color color) {
            this.argbColor = color.asARGB();
            return this;
        }

        @Override
        public Builder color(int argb) {
            this.argbColor = argb;
            return this;
        }

        @Override
        public Builder doubleSided(boolean value) {
            this.doubleSided = value;
            return this;
        }

        @Override
        public Builder brightness(int blockLight, int skyLight) {
            this.blockLight = blockLight;
            this.skyLight = skyLight;
            return this;
        }

        @Override
        public Builder seeThrough(boolean value) {
            this.seeThrough = value;
            return this;
        }

        @Override
        public Builder viewRange(float value) {
            this.viewRange = value;
            return this;
        }

        @Override
        public Builder rootAnchor(boolean value) {
            this.rootAnchorEnabled = value;
            return this;
        }

        @Override
        public PacketTriangle build() {
            return new PacketTriangle(this);
        }
    }
}
