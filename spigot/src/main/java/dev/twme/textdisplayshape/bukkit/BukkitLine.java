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
 * Line implementation using Bukkit API (Spigot-compatible).
 * Uses {@code setText()} instead of Paper's Adventure API.
 */
public class BukkitLine implements Shape {

    private Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final float thickness;
    private final float roll;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;
    private final float viewRange;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitLine(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.color = Color.fromARGB(builder.argbColor);
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
    }

    @Override
    public void spawn() {
        if (spawned) return;
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(p1, p2, thickness, roll);
        spawnTextDisplay(matrix);
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayLine(p2, p1, thickness, -roll);
            spawnTextDisplay(backMatrix);
        }
        spawned = true;
    }

    private void spawnTextDisplay(Matrix4f matrix) {
        Matrix4f adjustedMatrix = new Matrix4f()
                .translate((float) -origin.getX(), (float) -origin.getY(), (float) -origin.getZ())
                .mul(matrix);
        TextDisplay display = origin.getWorld().spawn(origin, TextDisplay.class, (d) -> {
            d.setText(" ");
            d.setBackgroundColor(color);
            d.setBrightness(new Display.Brightness(blockLight, skyLight));
            d.setTransformationMatrix(adjustedMatrix);
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

    public static class Builder implements ShapeBuilder<BukkitLine> {
        private final Location origin;
        private final Vector3f p1, p2;
        private final float thickness;
        private float roll = 0f;
        private int argbColor = Color.fromARGB(200, 255, 100, 100).asARGB();
        private boolean doubleSided = false;
        private int blockLight = 15, skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;

        public Builder(Location origin, Vector3f p1, Vector3f p2, float thickness) {
            this.origin = origin; this.p1 = p1; this.p2 = p2; this.thickness = thickness;
        }
        public Builder roll(float roll) { this.roll = roll; return this; }
        public Builder rollDegrees(float degrees) { this.roll = (float) Math.toRadians(degrees); return this; }
        public Builder color(Color color) { this.argbColor = color.asARGB(); return this; }
        @Override public Builder color(int argb) { this.argbColor = argb; return this; }
        @Override public Builder doubleSided(boolean v) { this.doubleSided = v; return this; }
        @Override public Builder brightness(int b, int s) { this.blockLight = b; this.skyLight = s; return this; }
        @Override public Builder seeThrough(boolean v) { this.seeThrough = v; return this; }
        @Override public Builder viewRange(float v) { this.viewRange = v; return this; }
        @Override public BukkitLine build() { return new BukkitLine(this); }
    }
}
