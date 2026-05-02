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
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Triangle implementation using Bukkit API to directly manipulate TextDisplay
 * entities.
 */
public class BukkitTriangle implements Shape {

    private Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final Vector3f p3;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;
    private final float viewRange;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitTriangle(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.p3 = builder.p3;
        this.color = Color.fromARGB(builder.argbColor);
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight;
        this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
    }

    @Override
    public void spawn() {
        if (spawned)
            return;

        // Front face: p1, p2, p3 — use analytical TRS for precision
        for (TRSResult trs : TextDisplayUtil.computeTriangleTRS(p1, p2, p3)) {
            spawnTextDisplay(trs);
        }

        // Back face: swap p2 and p3
        if (doubleSided) {
            for (TRSResult trs : TextDisplayUtil.computeTriangleTRS(p1, p3, p2)) {
                spawnTextDisplay(trs);
            }
        }

        spawned = true;
    }

    private void spawnTextDisplay(TRSResult trs) {
        // Adjust translation: convert from absolute world coordinates to relative to spawn location
        Vector3f adjustedTranslation = new Vector3f(trs.translation())
                .sub((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());

        Transformation transformation = new Transformation(
                adjustedTranslation,
                trs.leftRotation(),
                trs.scale(),
                trs.rightRotation());

        TextDisplay display = origin.getWorld().spawn(origin, TextDisplay.class, (d) -> {
            d.text(MiniMessage.miniMessage().deserialize(" "));
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
        // In Bukkit implementation, all players can see the entity
        // This method is reserved for packet mode
    }

    @Override
    public void removeViewer(UUID playerUUID) {
        // In Bukkit implementation, cannot control individual player visibility
        // This method is reserved for packet mode
    }

    @Override
    public Set<UUID> getViewerUUIDs() {
        // Returns empty set since Bukkit mode is visible to all players
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

    @Override
    public void teleportOrigin(double x, double y, double z) {
        if (!spawned) return;

        Location newOrigin = new Location(origin.getWorld(), x, y, z);

        float deltaX = (float) (x - origin.getX());
        float deltaY = (float) (y - origin.getY());
        float deltaZ = (float) (z - origin.getZ());

        for (TextDisplay display : displays) {
            if (!display.isValid()) continue;
            org.bukkit.util.Transformation t = display.getTransformation();
            org.joml.Vector3f tr = t.getTranslation();
            display.setTransformation(new org.bukkit.util.Transformation(
                    new org.joml.Vector3f(tr.x - deltaX, tr.y - deltaY, tr.z - deltaZ),
                    t.getLeftRotation(), t.getScale(), t.getRightRotation()));
            display.teleport(newOrigin);
        }

        this.origin = newOrigin.clone();
    }

    /**
     * Builder class.
     */
    public static class Builder implements ShapeBuilder<BukkitTriangle> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final Vector3f p3;

        private int argbColor = Color.fromARGB(150, 50, 100, 100).asARGB();
        private boolean doubleSided = false;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;

        public Builder(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
            this.origin = origin;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        /**
         * Sets the background color using a Bukkit Color.
         *
         * @param color the Bukkit color (including ARGB)
         * @return this builder
         */
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
        public Builder viewRange(float viewRange) {
            this.viewRange = viewRange;
            return this;
        }

        @Override
        public BukkitTriangle build() {
            return new BukkitTriangle(this);
        }
    }
}
