package dev.twme.textdisplayshape.bukkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TextDisplayUtil;

/**
 * Polyline (connected line segments) implementation using Bukkit API (Spigot-compatible).
 * Uses {@code setText()} instead of Paper's Adventure API.
 */
public class BukkitPolyline implements Shape {

    private Location origin;
    private final List<Vector3f> points;
    private final float thickness;
    private final float roll;
    private final Color color;
    private final boolean doubleSided;
    private final boolean closed;
    private final int blockLight, skyLight;
    private final boolean seeThrough;
    private final float viewRange;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitPolyline(Builder builder) {
        this.origin = builder.origin;
        this.points = new ArrayList<>(builder.points);
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.color = Color.fromARGB(builder.argbColor);
        this.doubleSided = builder.doubleSided;
        this.closed = builder.closed;
        this.blockLight = builder.blockLight; this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
    }

    @Override
    public void spawn() {
        if (spawned) return;
        if (points.size() < 2) { spawned = true; return; }
        for (int i = 0; i < points.size() - 1; i++) { spawnLineSegment(points.get(i), points.get(i + 1)); }
        if (closed && points.size() > 2) { spawnLineSegment(points.get(points.size() - 1), points.get(0)); }
        spawned = true;
    }

    private void spawnLineSegment(Vector3f p1, Vector3f p2) {
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(p1, p2, thickness, roll);
        spawnTextDisplay(matrix);
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayLine(p2, p1, thickness, -roll);
            spawnTextDisplay(backMatrix);
        }
    }

    private void spawnTextDisplay(Matrix4f matrix) {
        Matrix4f adj = new Matrix4f()
                .translate((float) -origin.getX(), (float) -origin.getY(), (float) -origin.getZ())
                .mul(matrix);
        TextDisplay display = origin.getWorld().spawn(origin, TextDisplay.class, (d) -> {
            d.setText(" ");
            d.setBackgroundColor(color);
            d.setBrightness(new Display.Brightness(blockLight, skyLight));
            d.setTransformationMatrix(adj);
            d.setSeeThrough(seeThrough);
            d.setViewRange(viewRange);
        });
        displays.add(display);
    }

    @Override
    public void remove() {
        for (TextDisplay d : displays) { if (d.isValid()) d.remove(); }
        displays.clear();
        spawned = false;
    }

    @Override public boolean isSpawned() { return spawned; }
    @Override public void addViewer(UUID playerUUID) { }
    @Override public void removeViewer(UUID playerUUID) { }
    @Override public Set<UUID> getViewerUUIDs() { return new HashSet<>(); }

    @Override
    public List<UUID> getEntityUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (TextDisplay d : displays) uuids.add(d.getUniqueId());
        return uuids;
    }

    public List<TextDisplay> getEntities() { return new ArrayList<>(displays); }

    @Override
    public void teleportOrigin(double x, double y, double z) {
        if (!spawned) return;
        Location newOrigin = new Location(origin.getWorld(), x, y, z);
        float dx = (float)(x - origin.getX()), dy = (float)(y - origin.getY()), dz = (float)(z - origin.getZ());
        for (TextDisplay display : displays) {
            if (!display.isValid()) continue;
            org.bukkit.util.Transformation t = display.getTransformation();
            org.joml.Vector3f tr = t.getTranslation();
            display.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(tr.x - dx, tr.y - dy, tr.z - dz),
                    t.getLeftRotation(), t.getScale(), t.getRightRotation()));
            display.teleport(newOrigin);
        }
        this.origin = newOrigin.clone();
    }

    public int getSegmentCount() {
        if (points.size() < 2) return 0;
        return closed ? points.size() : points.size() - 1;
    }

    public static class Builder implements ShapeBuilder<BukkitPolyline> {
        private final Location origin;
        private final List<Vector3f> points;
        private final float thickness;
        private float roll = 0f;
        private int argbColor = Color.fromARGB(200, 255, 100, 100).asARGB();
        private boolean doubleSided = false;
        private boolean closed = false;
        private int blockLight = 15, skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;

        public Builder(Location origin, List<Vector3f> points, float thickness) {
            this.origin = origin; this.points = new ArrayList<>(points); this.thickness = thickness;
        }
        public Builder closed(boolean v) { this.closed = v; return this; }
        public Builder roll(float v) { this.roll = v; return this; }
        public Builder rollDegrees(float degrees) { this.roll = (float) Math.toRadians(degrees); return this; }
        public Builder color(Color color) { this.argbColor = color.asARGB(); return this; }
        @Override public Builder color(int argb) { this.argbColor = argb; return this; }
        @Override public Builder doubleSided(boolean v) { this.doubleSided = v; return this; }
        @Override public Builder brightness(int b, int s) { this.blockLight = b; this.skyLight = s; return this; }
        @Override public Builder seeThrough(boolean v) { this.seeThrough = v; return this; }
        @Override public Builder viewRange(float v) { this.viewRange = v; return this; }
        @Override public BukkitPolyline build() { return new BukkitPolyline(this); }
    }
}
