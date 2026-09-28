package dev.twme.textdisplayshape.bukkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.geometry.DisplayTransform;
import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.shape.ShapeStyle;

/**
 * Shared lifecycle, animation, and movement handling for shapes made of
 * server-side Text Display entities (Spigot-compatible, no Adventure API). Subclasses only describe
 * their geometry as world-space {@link DisplayTransform}s.
 *
 * <p>All methods must run on the thread that owns the shape's entities.
 * Direct entities are visible to every player, so viewer methods are
 * no-ops.</p>
 */
public abstract class AbstractBukkitShape implements Shape {

    private Location origin;
    private int argbColor;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;
    private final float viewRange;
    private int interpolationDuration;
    private int teleportDuration;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned;

    protected AbstractBukkitShape(Builder<?, ?> builder) {
        this.origin = Objects.requireNonNull(builder.origin, "origin").clone();
        this.argbColor = builder.argbColor;
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
        this.interpolationDuration = builder.interpolationDuration;
        this.teleportDuration = builder.teleportDuration;
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

    @Override
    public void spawn() {
        if (spawned) {
            return;
        }
        for (DisplayTransform transform : computeTransforms()) {
            spawnDisplay(transform);
        }
        spawned = true;
    }

    private void spawnDisplay(DisplayTransform transform) {
        Transformation transformation = toTransformation(
                transform.relativeTo(origin.getX(), origin.getY(), origin.getZ()));
        TextDisplay display = Objects.requireNonNull(origin.getWorld(), "origin world")
                .spawn(origin, TextDisplay.class, entity -> {
                    entity.setText(" ");
                    entity.setBackgroundColor(Color.fromARGB(argbColor));
                    entity.setBrightness(new Display.Brightness(blockLight, skyLight));
                    entity.setTransformation(transformation);
                    entity.setSeeThrough(seeThrough);
                    entity.setViewRange(viewRange);
                    entity.setTeleportDuration(teleportDuration);
                });
        displays.add(display);
    }

    @Override
    public void remove() {
        for (TextDisplay display : displays) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
        spawned = false;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public void addViewer(UUID playerUUID) {
    }

    @Override
    public void removeViewer(UUID playerUUID) {
    }

    @Override
    public Set<UUID> getViewerUUIDs() {
        return new HashSet<>();
    }

    @Override
    public List<UUID> getEntityUUIDs() {
        List<UUID> uuids = new ArrayList<>(displays.size());
        for (TextDisplay display : displays) {
            uuids.add(display.getUniqueId());
        }
        return uuids;
    }

    @Override
    public int getEntityCount() {
        return displays.size();
    }

    public List<TextDisplay> getEntities() {
        return new ArrayList<>(displays);
    }

    /**
     * Sends new geometry. Existing entities are updated in place and animate;
     * entities needed by a larger geometry appear immediately, and surplus
     * entities are removed.
     */
    protected final void applyTransforms(List<DisplayTransform> transforms) {
        if (!spawned) {
            return;
        }
        int reused = Math.min(displays.size(), transforms.size());
        for (int index = 0; index < reused; index++) {
            TextDisplay display = displays.get(index);
            if (!display.isValid()) {
                continue;
            }
            startInterpolation(display);
            display.setTransformation(toTransformation(
                    transforms.get(index).relativeTo(origin.getX(), origin.getY(), origin.getZ())));
        }
        while (displays.size() > transforms.size()) {
            TextDisplay surplus = displays.remove(displays.size() - 1);
            if (surplus.isValid()) {
                surplus.remove();
            }
        }
        for (int index = reused; index < transforms.size(); index++) {
            spawnDisplay(transforms.get(index));
        }
    }

    private void startInterpolation(TextDisplay display) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(interpolationDuration);
    }

    @Override
    public void setColor(int argb) {
        this.argbColor = argb;
        for (TextDisplay display : displays) {
            if (display.isValid()) {
                startInterpolation(display);
                display.setBackgroundColor(Color.fromARGB(argb));
            }
        }
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
        for (TextDisplay display : displays) {
            if (display.isValid()) {
                display.setTeleportDuration(ticks);
            }
        }
    }

    @Override
    public int getTeleportDuration() {
        return teleportDuration;
    }

    @Override
    public void translate(double dx, double dy, double dz) {
        offsetGeometry((float) dx, (float) dy, (float) dz);
        origin = origin.clone().add(dx, dy, dz);
        for (TextDisplay display : displays) {
            if (display.isValid()) {
                display.teleport(origin);
            }
        }
    }

    /**
     * Rebases the entities without moving the rendered geometry. Rebase with
     * a zero teleport duration: Bukkit synchronizes entity data once per
     * tick, so an animated teleport would briefly show the entities moving.
     */
    @Override
    public void teleportOrigin(double x, double y, double z) {
        if (!spawned) {
            return;
        }
        Location newOrigin = new Location(origin.getWorld(), x, y, z);
        float dx = (float) (x - origin.getX());
        float dy = (float) (y - origin.getY());
        float dz = (float) (z - origin.getZ());
        for (TextDisplay display : displays) {
            if (!display.isValid()) {
                continue;
            }
            Transformation current = display.getTransformation();
            Vector3f translation = current.getTranslation();
            display.setInterpolationDuration(0);
            display.setTransformation(new Transformation(
                    new Vector3f(translation.x - dx, translation.y - dy, translation.z - dz),
                    current.getLeftRotation(), current.getScale(), current.getRightRotation()));
            display.teleport(newOrigin);
        }
        origin = newOrigin.clone();
    }

    private static Transformation toTransformation(DisplayTransform transform) {
        return new Transformation(
                transform.translation(),
                transform.leftRotation(),
                transform.scale(),
                transform.rightRotation());
    }

    /**
     * Shared builder settings.
     *
     * @param <S> the shape type
     * @param <B> the concrete builder type
     */
    public abstract static class Builder<S extends AbstractBukkitShape, B extends Builder<S, B>>
            implements ShapeBuilder<S> {
        private final Location origin;
        private int argbColor = Color.fromARGB(200, 255, 100, 100).asARGB();
        private boolean doubleSided;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;
        private int interpolationDuration;
        private int teleportDuration;

        protected Builder(Location origin) {
            this.origin = origin;
        }

        @SuppressWarnings("unchecked")
        protected final B self() {
            return (B) this;
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
            if (blockLight < 0 || blockLight > 15 || skyLight < 0 || skyLight > 15) {
                throw new IllegalArgumentException("Light levels must be between 0 and 15");
            }
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
