package dev.twme.textdisplayshape.bukkit;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TextDisplayUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Polyline (connected line segments) implementation using Bukkit API.
 * Creates multiple connected line segments from a list of points.
 */
public class BukkitPolyline implements Shape {

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

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitPolyline(Builder builder) {
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
        spawnTextDisplay(matrix);

        // Back face
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayLine(p2, p1, thickness, -roll);
            spawnTextDisplay(backMatrix);
        }
    }

    private void spawnTextDisplay(Matrix4f matrix) {
        Matrix4f adjustedMatrix = new Matrix4f()
                .translate(
                        (float) -origin.getX(),
                        (float) -origin.getY(),
                        (float) -origin.getZ())
                .mul(matrix);

        TextDisplay display = origin.getWorld().spawn(origin, TextDisplay.class, (d) -> {
            d.text(MiniMessage.miniMessage().deserialize(" "));
            d.setBackgroundColor(color);
            d.setBrightness(new Display.Brightness(blockLight, skyLight));
            d.setTransformationMatrix(adjustedMatrix);
            d.setSeeThrough(seeThrough);
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
    public void addViewer(Player player) {
        // In Bukkit implementation, all players can see the entity
    }

    @Override
    public void removeViewer(Player player) {
        // In Bukkit implementation, cannot control individual player visibility
    }

    @Override
    public Set<Player> getViewers() {
        return new HashSet<>();
    }

    @Override
    public List<UUID> getEntityUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (TextDisplay display : displays) {
            uuids.add(display.getUniqueId());
        }
        return uuids;
    }

    /**
     * Gets all TextDisplay entities of this shape.
     *
     * @return list of TextDisplay entities
     */
    public List<TextDisplay> getEntities() {
        return new ArrayList<>(displays);
    }

    /**
     * Gets the number of line segments in this polyline.
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
    public static class Builder implements ShapeBuilder<BukkitPolyline> {
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

        /**
         * Creates a polyline builder with the given points.
         *
         * @param origin    the spawn location
         * @param points    the list of points defining the polyline (at least 2 points
         *                  required)
         * @param thickness the line thickness
         */
        public Builder(Location origin, List<Vector3f> points, float thickness) {
            this.origin = origin;
            this.points = new ArrayList<>(points);
            this.thickness = thickness;
        }

        /**
         * Sets whether the polyline should be closed (connect last point to first).
         *
         * @param closed true to close the polyline
         * @return this builder
         */
        public Builder closed(boolean closed) {
            this.closed = closed;
            return this;
        }

        /**
         * Sets the roll angle (rotation around the line axis).
         *
         * @param roll the roll angle in radians
         * @return this builder
         */
        public Builder roll(float roll) {
            this.roll = roll;
            return this;
        }

        /**
         * Sets the roll angle in degrees.
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
        public BukkitPolyline build() {
            return new BukkitPolyline(this);
        }
    }
}
