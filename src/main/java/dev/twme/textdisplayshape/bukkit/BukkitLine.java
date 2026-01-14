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
 * 使用 Bukkit API 直接操作 TextDisplay 實體的線條實作。
 */
public class BukkitLine implements Shape {

    private final Location origin;
    private final Vector3f p1;
    private final Vector3f p2;
    private final float thickness;
    private final Color color;
    private final boolean doubleSided;
    private final int blockLight;
    private final int skyLight;
    private final boolean seeThrough;

    private final List<TextDisplay> displays = new ArrayList<>();
    private boolean spawned = false;

    private BukkitLine(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1;
        this.p2 = builder.p2;
        this.thickness = builder.thickness;
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

        // 正面：p1 -> p2
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(p1, p2, thickness);
        spawnTextDisplay(matrix);

        // 背面：交換 p1 和 p2
        if (doubleSided) {
            Matrix4f backMatrix = TextDisplayUtil.textDisplayLine(p2, p1, thickness);
            spawnTextDisplay(backMatrix);
        }

        spawned = true;
    }

    private void spawnTextDisplay(Matrix4f matrix) {
        // 調整變換矩陣：將絕對座標轉換為相對於生成位置的座標
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
        // Bukkit 實作中，所有玩家都可以看到實體
    }

    @Override
    public void removeViewer(Player player) {
        // Bukkit 實作中，無法控制個別玩家的可見性
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
     * 獲取此形狀的所有 TextDisplay 實體。
     *
     * @return TextDisplay 實體列表
     */
    public List<TextDisplay> getEntities() {
        return new ArrayList<>(displays);
    }

    /**
     * 建造者類別。
     */
    public static class Builder implements ShapeBuilder<BukkitLine> {
        private final Location origin;
        private final Vector3f p1;
        private final Vector3f p2;
        private final float thickness;

        private Color color = Color.fromARGB(200, 255, 100, 100);
        private boolean doubleSided = false;
        private int blockLight = 15;
        private int skyLight = 15;
        private boolean seeThrough = true;

        public Builder(Location origin, Vector3f p1, Vector3f p2, float thickness) {
            this.origin = origin;
            this.p1 = p1;
            this.p2 = p2;
            this.thickness = thickness;
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
        public BukkitLine build() {
            return new BukkitLine(this);
        }
    }
}
