package dev.twme.textdisplayshape.packet;

import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TextDisplayUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Polyline implementation using VirtualEntities and PacketEvents. */
public class PacketPolyline implements Shape {
    private Location origin;
    private final List<Vector3f> points;
    private final float thickness;
    private final float roll;
    private final int argbColor;
    private final boolean doubleSided;
    private final boolean closed;
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

    private PacketPolyline(Builder builder) {
        this.origin = builder.origin;
        this.points = new ArrayList<>(builder.points);
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.argbColor = builder.argbColor;
        this.doubleSided = builder.doubleSided;
        this.closed = builder.closed;
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
        if (points.size() < 2) {
            spawned = true;
            return;
        }
        if (rootAnchorEnabled) {
            rootAnchor = PacketRootAnchorSupport.createRootAnchor(entityManager, origin, viewRange, Set.of());
        }
        for (int index = 0; index < points.size() - 1; index++) {
            spawnLineSegment(points.get(index), points.get(index + 1));
        }
        if (closed && points.size() > 2) {
            spawnLineSegment(points.get(points.size() - 1), points.get(0));
        }
        if (rootAnchor != null) {
            VirtualTextDisplaySupport.addViewers(entityManager, rootAnchor, viewerUUIDs);
        }
        spawned = true;
    }

    private void spawnLineSegment(Vector3f first, Vector3f second) {
        createVirtualEntity(TextDisplayUtil.textDisplayLine(first, second, thickness, roll));
        if (doubleSided) {
            createVirtualEntity(TextDisplayUtil.textDisplayLine(second, first, thickness, roll + (float) Math.PI));
        }
    }

    private void createVirtualEntity(Matrix4f matrix) {
        Matrix4f adjusted = new Matrix4f()
                .translate((float) -origin.getX(), (float) -origin.getY(), (float) -origin.getZ())
                .mul(matrix);
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
                    VirtualTextDisplaySupport.setTransformFromMatrix(virtualEntity, adjusted);
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

    public int getSegmentCount() {
        if (points.size() < 2) {
            return 0;
        }
        return closed ? points.size() : points.size() - 1;
    }

    public static class Builder implements ShapeBuilder<PacketPolyline> {
        private final Location origin;
        private final List<Vector3f> points;
        private final float thickness;
        private float roll;
        private int argbColor = Color.fromARGB(200, 255, 100, 100).asARGB();
        private boolean doubleSided;
        private boolean closed;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;
        private boolean rootAnchorEnabled;
        private VirtualEntityManager entityManager = PacketEntityManagers.defaultManager();

        public Builder(Location origin, List<Vector3f> points, float thickness) {
            this.origin = origin;
            this.points = new ArrayList<>(points);
            this.thickness = thickness;
        }

        Builder withEntityManager(VirtualEntityManager entityManager) {
            this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
            return this;
        }

        public Builder closed(boolean value) {
            this.closed = value;
            return this;
        }

        public Builder roll(float value) {
            this.roll = value;
            return this;
        }

        public Builder rollDegrees(float degrees) {
            this.roll = (float) Math.toRadians(degrees);
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
        public PacketPolyline build() {
            return new PacketPolyline(this);
        }
    }
}
