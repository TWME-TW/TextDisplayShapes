package dev.twme.textdisplayshape.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TextDisplayUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;

/**
 * Polyline (connected line segments) implementation using EntityLib packets.
 *
 * <p><b>Memory safety:</b> Viewers are tracked by UUID only.</p>
 */
public class PacketPolyline implements Shape {

    private Location origin;
    private final List<Vector3f> points;
    private final float thickness;
    private final float roll;
    private final int argbColor;
    private final boolean doubleSided;
    private final boolean closed;
    private final int blockLight, skyLight;
    private final boolean seeThrough;
    private final float viewRange;
    private final boolean rootAnchorEnabled;

    private final List<WrapperEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private WrapperEntity rootAnchor;
    private boolean spawned = false;

    private PacketPolyline(Builder builder) {
        this.origin = builder.origin;
        this.points = new ArrayList<>(builder.points);
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.argbColor = builder.argbColor;
        this.doubleSided = builder.doubleSided;
        this.closed = builder.closed;
        this.blockLight = builder.blockLight; this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
        this.rootAnchorEnabled = builder.rootAnchorEnabled;
    }

    @Override
    public void spawn() {
        if (spawned) return;
        if (rootAnchorEnabled && points.size() >= 2) { rootAnchor = PacketRootAnchorSupport.createRootAnchor(origin, viewRange, viewerUUIDs); }
        if (points.size() < 2) { spawned = true; return; }
        for (int i = 0; i < points.size() - 1; i++) { spawnLineSegment(points.get(i), points.get(i + 1)); }
        if (closed && points.size() > 2) { spawnLineSegment(points.get(points.size() - 1), points.get(0)); }
        spawned = true;
    }

    private void spawnLineSegment(Vector3f p1, Vector3f p2) {
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(p1, p2, thickness, roll);
        createWrapperEntity(matrix);
        if (doubleSided) {
            Matrix4f back = TextDisplayUtil.textDisplayLine(p2, p1, thickness, roll + (float) Math.PI);
            createWrapperEntity(back);
        }
    }

    private void createWrapperEntity(Matrix4f matrix) {
        Matrix4f adj = new Matrix4f().translate((float) -origin.getX(), (float) -origin.getY(), (float) -origin.getZ()).mul(matrix);
        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(origin));
        if (entity.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setText(net.kyori.adventure.text.Component.text(" "));
            meta.setBackgroundColor(argbColor);
            meta.setSeeThrough(seeThrough);
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta dm) {
                dm.setBrightnessOverride(blockLight << 4 | skyLight << 20);
                dm.setViewRange(viewRange);
            }
            setTransformFromMatrix(entity, adj);
        }
        for (UUID uuid : viewerUUIDs) { entity.addViewer(uuid); }
        entities.add(entity);
        if (rootAnchorEnabled) { PacketRootAnchorSupport.attachPassenger(rootAnchor, entity); }
    }

    private void setTransformFromMatrix(WrapperEntity entity, Matrix4f matrix) {
        if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta meta)) return;
        Vector3f t = new Vector3f(); matrix.getTranslation(t);
        Vector3f s = new Vector3f(); matrix.getScale(s);
        org.joml.Quaternionf r = new org.joml.Quaternionf(); matrix.getUnnormalizedRotation(r);
        meta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(t.x, t.y, t.z));
        meta.setScale(new com.github.retrooper.packetevents.util.Vector3f(s.x, s.y, s.z));
        meta.setLeftRotation(new com.github.retrooper.packetevents.util.Quaternion4f(r.x, r.y, r.z, r.w));
    }

    @Override public void remove() { for (WrapperEntity e : entities) e.remove(); entities.clear(); if (rootAnchor != null) { rootAnchor.remove(); rootAnchor = null; } spawned = false; }
    @Override public boolean isSpawned() { return spawned; }

    @Override
    public void addViewer(UUID playerUUID) {
        viewerUUIDs.add(playerUUID);
        if (spawned) { if (rootAnchor != null) rootAnchor.addViewer(playerUUID); for (WrapperEntity e : entities) e.addViewer(playerUUID); }
    }

    @Override
    public void removeViewer(UUID playerUUID) {
        viewerUUIDs.remove(playerUUID);
        if (spawned) { if (rootAnchor != null) rootAnchor.removeViewer(playerUUID); for (WrapperEntity e : entities) e.removeViewer(playerUUID); }
    }

    @Override public Set<UUID> getViewerUUIDs() { return new HashSet<>(viewerUUIDs); }
    @Override public List<UUID> getEntityUUIDs() { List<UUID> u = new ArrayList<>(); for (WrapperEntity e : entities) u.add(e.getUuid()); return u; }
    public List<WrapperEntity> getEntities() { return new ArrayList<>(entities); }

    @Override
    public void teleportOrigin(double x, double y, double z) {
        if (!spawned) return;
        Location newOrigin = new Location(origin.getWorld(), x, y, z);
        if (rootAnchorEnabled && rootAnchor != null) { PacketRootAnchorSupport.teleportRootAnchor(rootAnchor, entities, origin, newOrigin); this.origin = newOrigin.clone(); return; }
        float dx = (float)(x-origin.getX()), dy = (float)(y-origin.getY()), dz = (float)(z-origin.getZ());
        com.github.retrooper.packetevents.protocol.world.Location peLoc = SpigotConversionUtil.fromBukkitLocation(newOrigin);
        for (WrapperEntity entity : entities) {
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta dm) {
                com.github.retrooper.packetevents.util.Vector3f old = dm.getTranslation();
                dm.setInterpolationDelay(0); dm.setTransformationInterpolationDuration(0); dm.setPositionRotationInterpolationDuration(0);
                dm.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(old.getX()-dx, old.getY()-dy, old.getZ()-dz));
                entity.sendPacketToViewers(new WrapperPlayServerBundle()); entity.sendPacketToViewers(entity.getEntityMeta().createPacket());
                entity.sendPacketToViewers(new WrapperPlayServerEntityTeleport(entity.getEntityId(), peLoc.getPosition(), peLoc.getYaw(), peLoc.getPitch(), false));
                entity.sendPacketToViewers(new WrapperPlayServerBundle()); entity.setLocation(peLoc);
            } else { entity.teleport(peLoc); }
        }
        this.origin = newOrigin.clone();
    }

    public int getSegmentCount() { if (points.size() < 2) return 0; return closed ? points.size() : points.size() - 1; }

    public static class Builder implements ShapeBuilder<PacketPolyline> {
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
        private boolean rootAnchorEnabled = false;

        public Builder(Location origin, List<Vector3f> points, float thickness) { this.origin = origin; this.points = new ArrayList<>(points); this.thickness = thickness; }
        public Builder closed(boolean v) { this.closed = v; return this; }
        public Builder roll(float v) { this.roll = v; return this; }
        public Builder rollDegrees(float degrees) { this.roll = (float) Math.toRadians(degrees); return this; }
        public Builder color(Color color) { this.argbColor = color.asARGB(); return this; }
        @Override public Builder color(int argb) { this.argbColor = argb; return this; }
        @Override public Builder doubleSided(boolean v) { this.doubleSided = v; return this; }
        @Override public Builder brightness(int b, int s) { this.blockLight = b; this.skyLight = s; return this; }
        @Override public Builder seeThrough(boolean v) { this.seeThrough = v; return this; }
        @Override public Builder viewRange(float v) { this.viewRange = v; return this; }
        @Override public Builder rootAnchor(boolean v) { this.rootAnchorEnabled = v; return this; }
        @Override public PacketPolyline build() { return new PacketPolyline(this); }
    }
}
