package dev.twme.textdisplayshape.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;

import dev.twme.textdisplayshape.geometry.DisplayTransform;
import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.shape.ShapeStyle;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;

/**
 * Shared lifecycle, viewer, animation, and movement handling for packet
 * shapes. Subclasses only describe their geometry as world-space
 * {@link DisplayTransform}s.
 *
 * <p>Updates reuse the existing virtual entities and are sent in one
 * VirtualEntities bundle, so every part of a shape changes in the same frame
 * and animates over {@link #getInterpolationDuration()} ticks.</p>
 */
public abstract class AbstractPacketShape implements Shape {

    private Location origin;
    private int argbColor;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;
    private final float viewRange;
    private final boolean rootAnchorEnabled;
    private int interpolationDuration;
    private int teleportDuration;
    private final VirtualEntityManager entityManager;

    private final List<VirtualEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private VirtualEntity rootAnchor;
    private boolean spawned;

    protected AbstractPacketShape(Builder<?, ?> builder) {
        this.origin = Objects.requireNonNull(builder.origin, "origin").clone();
        this.argbColor = builder.argbColor;
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
        this.rootAnchorEnabled = builder.rootAnchorEnabled;
        this.interpolationDuration = builder.interpolationDuration;
        this.teleportDuration = builder.teleportDuration;
        this.entityManager = builder.entityManager;
    }

    /** World-space transforms for every entity of the current geometry. */
    protected abstract List<DisplayTransform> computeTransforms();

    /** Shifts the stored geometry by a world-space offset. */
    protected abstract void offsetGeometry(float dx, float dy, float dz);

    protected final boolean isDoubleSided() {
        return doubleSided;
    }

    /** The current logical origin of the shape's entities. */
    public Location getOrigin() {
        return origin.clone();
    }

    // =====================================================================
    //  Lifecycle
    // =====================================================================

    @Override
    public void spawn() {
        if (spawned) {
            return;
        }
        List<DisplayTransform> transforms = computeTransforms();
        if (rootAnchorEnabled && !transforms.isEmpty()) {
            rootAnchor = PacketRootAnchorSupport.createRootAnchor(entityManager, origin, viewRange, Set.of());
            VirtualTextDisplaySupport.setTeleportDuration(rootAnchor, teleportDuration);
        }
        for (DisplayTransform transform : transforms) {
            createEntity(transform);
        }
        if (rootAnchor != null) {
            VirtualTextDisplaySupport.addViewers(entityManager, rootAnchor, viewerUUIDs);
        }
        spawned = true;
    }

    private void createEntity(DisplayTransform transform) {
        DisplayTransform relative = transform.relativeTo(origin.getX(), origin.getY(), origin.getZ());
        VirtualEntity entity = VirtualTextDisplaySupport.createTextDisplay(
                entityManager,
                origin,
                viewerUUIDs,
                virtualEntity -> {
                    VirtualTextDisplaySupport.configureTextDisplay(
                            virtualEntity, argbColor, seeThrough, blockLight, skyLight, viewRange);
                    VirtualTextDisplaySupport.setTransform(virtualEntity, relative);
                    if (!rootAnchorEnabled) {
                        VirtualTextDisplaySupport.setTeleportDuration(virtualEntity, teleportDuration);
                    }
                }
        );
        entities.add(entity);
        if (rootAnchor != null) {
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

    // =====================================================================
    //  Viewers
    // =====================================================================

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
        List<UUID> uuids = new ArrayList<>(entities.size());
        for (VirtualEntity entity : entities) {
            uuids.add(entity.uuid());
        }
        return uuids;
    }

    @Override
    public int getEntityCount() {
        return entities.size();
    }

    /** The Text Display entities of this shape, excluding the root anchor. */
    public List<VirtualEntity> getEntities() {
        return new ArrayList<>(entities);
    }

    /** The invisible root anchor, or {@code null} when root-anchor mode is off or not spawned. */
    public VirtualEntity getRootAnchor() {
        return rootAnchor;
    }

    // =====================================================================
    //  Updates
    // =====================================================================

    /**
     * Sends new geometry. Existing entities are updated in place and animate;
     * entities needed by a larger geometry appear immediately, and surplus
     * entities are removed.
     */
    protected final void applyTransforms(List<DisplayTransform> transforms) {
        if (!spawned) {
            return;
        }
        entityManager.bundle(() -> {
            int reused = Math.min(entities.size(), transforms.size());
            for (int index = 0; index < reused; index++) {
                VirtualEntity entity = entities.get(index);
                VirtualTextDisplaySupport.setTransform(entity,
                        transforms.get(index).relativeTo(origin.getX(), origin.getY(), origin.getZ()));
                VirtualTextDisplaySupport.setInterpolation(entity, interpolationDuration);
                entity.syncMetadata();
            }
            while (entities.size() > transforms.size()) {
                entities.remove(entities.size() - 1).remove();
            }
            if (rootAnchorEnabled && rootAnchor == null && transforms.size() > reused) {
                rootAnchor = PacketRootAnchorSupport.createRootAnchor(entityManager, origin, viewRange, Set.of());
                VirtualTextDisplaySupport.setTeleportDuration(rootAnchor, teleportDuration);
                VirtualTextDisplaySupport.addViewers(entityManager, rootAnchor, viewerUUIDs);
            }
            for (int index = reused; index < transforms.size(); index++) {
                createEntity(transforms.get(index));
            }
        });
    }

    @Override
    public void setColor(int argb) {
        this.argbColor = argb;
        if (!spawned) {
            return;
        }
        entityManager.bundle(() -> {
            for (VirtualEntity entity : entities) {
                VirtualTextDisplaySupport.setBackgroundColor(entity, argb);
                VirtualTextDisplaySupport.setInterpolation(entity, interpolationDuration);
                entity.syncMetadata();
            }
        });
    }

    @Override
    public int getColor() {
        return argbColor;
    }

    /** Convenience overload of {@link #setColor(int)}. */
    public void setColor(Color color) {
        setColor(color.asARGB());
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must not be negative");
        }
        this.interpolationDuration = ticks;
    }

    @Override
    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    @Override
    public void setTeleportDuration(int ticks) {
        if (ticks < 0 || ticks > MAX_TELEPORT_DURATION) {
            throw new IllegalArgumentException("ticks must be between 0 and " + MAX_TELEPORT_DURATION);
        }
        this.teleportDuration = ticks;
    }

    @Override
    public int getTeleportDuration() {
        return teleportDuration;
    }

    @Override
    public void translate(double dx, double dy, double dz) {
        offsetGeometry((float) dx, (float) dy, (float) dz);
        Location newOrigin = origin.clone().add(dx, dy, dz);
        origin = newOrigin;
        if (!spawned) {
            return;
        }
        var target = SpigotConversionUtil.fromBukkitLocation(newOrigin);
        entityManager.bundle(() -> {
            for (VirtualEntity mover : movers()) {
                VirtualTextDisplaySupport.setTeleportDuration(mover, teleportDuration);
                mover.syncMetadata();
                mover.teleport(target);
            }
        });
    }

    private List<VirtualEntity> movers() {
        return rootAnchor != null ? List.of(rootAnchor) : entities;
    }

    @Override
    public void teleportOrigin(double x, double y, double z) {
        if (!spawned) {
            return;
        }
        Location newOrigin = new Location(origin.getWorld(), x, y, z);
        if (rootAnchor != null) {
            PacketRootAnchorSupport.teleportRootAnchor(entityManager, rootAnchor, entities, origin, newOrigin);
        } else {
            VirtualTextDisplaySupport.rebaseEntities(entityManager, entities, origin, newOrigin);
        }
        origin = newOrigin.clone();
    }

    // =====================================================================
    //  Builder
    // =====================================================================

    /**
     * Shared builder settings.
     *
     * @param <S> the shape type
     * @param <B> the concrete builder type
     */
    public abstract static class Builder<S extends AbstractPacketShape, B extends Builder<S, B>>
            implements ShapeBuilder<S> {
        private final Location origin;
        private int argbColor = Color.fromARGB(200, 255, 100, 100).asARGB();
        private boolean doubleSided;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;
        private boolean rootAnchorEnabled;
        private int interpolationDuration;
        private int teleportDuration;
        private VirtualEntityManager entityManager = PacketEntityManagers.defaultManager();

        protected Builder(Location origin) {
            this.origin = origin;
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
        }

        B withEntityManager(VirtualEntityManager manager) {
            this.entityManager = Objects.requireNonNull(manager, "entityManager");
            return self();
        }

        public B color(Color color) {
            this.argbColor = color.asARGB();
            return self();
        }

        @Override
        public B color(int argb) {
            this.argbColor = argb;
            return self();
        }

        @Override
        public B doubleSided(boolean value) {
            this.doubleSided = value;
            return self();
        }

        @Override
        public B brightness(int blockLight, int skyLight) {
            VirtualTextDisplaySupport.packBrightness(blockLight, skyLight);
            this.blockLight = blockLight;
            this.skyLight = skyLight;
            return self();
        }

        @Override
        public B seeThrough(boolean value) {
            this.seeThrough = value;
            return self();
        }

        @Override
        public B viewRange(float value) {
            this.viewRange = value;
            return self();
        }

        @Override
        public B rootAnchor(boolean value) {
            this.rootAnchorEnabled = value;
            return self();
        }

        @Override
        public B interpolationDuration(int ticks) {
            if (ticks < 0) {
                throw new IllegalArgumentException("ticks must not be negative");
            }
            this.interpolationDuration = ticks;
            return self();
        }

        @Override
        public B teleportDuration(int ticks) {
            if (ticks < 0 || ticks > MAX_TELEPORT_DURATION) {
                throw new IllegalArgumentException("ticks must be between 0 and " + MAX_TELEPORT_DURATION);
            }
            this.teleportDuration = ticks;
            return self();
        }

        @Override
        public B style(ShapeStyle style) {
            ShapeBuilder.super.style(style);
            return self();
        }
    }
}
