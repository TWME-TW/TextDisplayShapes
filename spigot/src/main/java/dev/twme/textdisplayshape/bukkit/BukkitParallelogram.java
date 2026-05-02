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
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TRSResult;
import dev.twme.textdisplayshape.util.TextDisplayUtil;

/**
 * Parallelogram implementation using Bukkit API (Spigot-compatible).
 * Uses {@code setText()} instead of Paper's Adventure API.
 */
public class BukkitParallelogram implements Shape {

    private Location origin;
    private final Vector3f p1, p2, p3;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight, skyLight;
    private final boolean seeThrough;
    private final float viewRange;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitParallelogram(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1; this.p2 = builder.p2; this.p3 = builder.p3;
        this.color = Color.fromARGB(builder.argbColor);
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight; this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
    }

    @Override
    public void spawn() {
        if (spawned) return;
        spawnTextDisplay(TextDisplayUtil.computeParallelogramTRS(p1, p2, p3));
        if (doubleSided) { spawnTextDisplay(TextDisplayUtil.computeParallelogramTRS(p1, p3, p2)); }
        spawned = true;
    }

    private void spawnTextDisplay(TRSResult trs) {
        Vector3f adj = new Vector3f(trs.translation()).sub((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());
        Transformation transformation = new Transformation(adj, trs.leftRotation(), trs.scale(), trs.rightRotation());
        TextDisplay display = origin.getWorld().spawn(origin, TextDisplay.class, (d) -> {
            d.setText(" ");
            d.setBackgroundColor(color);
            d.setBrightness(new Display.Brightness(blockLight, skyLight));
            d.setTransformation(transformation);
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

    public static class Builder implements ShapeBuilder<BukkitParallelogram> {
        private final Location origin;
        private final Vector3f p1, p2, p3;
        private int argbColor = Color.fromARGB(150, 100, 50, 150).asARGB();
        private boolean doubleSided = false;
        private int blockLight = 15, skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;

        public Builder(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
            this.origin = origin; this.p1 = p1; this.p2 = p2; this.p3 = p3;
        }
        public Builder color(Color color) { this.argbColor = color.asARGB(); return this; }
        @Override public Builder color(int argb) { this.argbColor = argb; return this; }
        @Override public Builder doubleSided(boolean v) { this.doubleSided = v; return this; }
        @Override public Builder brightness(int b, int s) { this.blockLight = b; this.skyLight = s; return this; }
        @Override public Builder seeThrough(boolean v) { this.seeThrough = v; return this; }
        @Override public Builder viewRange(float v) { this.viewRange = v; return this; }
        @Override public BukkitParallelogram build() { return new BukkitParallelogram(this); }
    }
}
