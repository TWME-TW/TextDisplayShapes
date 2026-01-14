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
 * Parallelogram implementation using Bukkit API to directly manipulate
 * TextDisplay entities.
 */
public class BukkitParallelogram implements Shape {

    private final Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final Vector3f p3;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitParallelogram(Builder builder) {
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

        // Front face: p1, p2, p3
        Matrix4f matrix = TextDisplayUtil.textDisplayParallelogram(p1, p2, p3);
        spawnTextDisplay(matrix);

        // Back face: swap p2 and p3
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayParallelogram(p1, p3, p2);
            spawnTextDisplay(backMatrix);
        }

        spawned = true;
    }

    private void spawnTextDisplay(Matrix4f matrix) {
        // Adjust transformation matrix: convert absolute coordinates to relative to
        // spawn location
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
     * Builder class.
     */
    public static class Builder implements ShapeBuilder<BukkitParallelogram> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final Vector3f p3;

        private Color color = Color.fromARGB(150, 100, 50, 150);
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
        public BukkitParallelogram build() {
            return new BukkitParallelogram(this);
        }
    }
}
