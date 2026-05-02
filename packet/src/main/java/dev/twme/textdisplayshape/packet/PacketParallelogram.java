package dev.twme.textdisplayshape.packet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Location;
import org.joml.Vector3f;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;

import dev.twme.textdisplayshape.shape.Shape;
import dev.twme.textdisplayshape.shape.ShapeBuilder;
import dev.twme.textdisplayshape.util.TRSResult;
import dev.twme.textdisplayshape.util.TextDisplayUtil;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;

/**
 * Parallelogram implementation using EntityLib packet-based display.
 *
 * <p><b>Memory safety:</b> Viewers are tracked by UUID only.</p>
 */
public class PacketParallelogram implements Shape {

    private Location origin;
    private final Vector3f p1, p2, p3;
    private final int argbColor;
    private final boolean doubleSided;
    private final int blockLight, skyLight;
    private final boolean seeThrough;
    private final float viewRange;
    private final boolean rootAnchorEnabled;

    private final List<WrapperEntity> entities = new ArrayList<>();
    private final Set<UUID> viewerUUIDs = new HashSet<>();
    private WrapperEntity rootAnchor;
    private boolean spawned = false;

    private PacketParallelogram(Builder builder) {
        this.origin = builder.origin;
        this.p1 = builder.p1; this.p2 = builder.p2; this.p3 = builder.p3;
        this.argbColor = builder.argbColor;
        this.doubleSided = builder.doubleSided;
        this.blockLight = builder.blockLight; this.skyLight = builder.skyLight;
        this.seeThrough = builder.seeThrough;
        this.viewRange = builder.viewRange;
        this.rootAnchorEnabled = builder.rootAnchorEnabled;
    }

    @Override
    public void spawn() {
        if (spawned) return;
        if (rootAnchorEnabled) { rootAnchor = PacketRootAnchorSupport.createRootAnchor(origin, viewRange, viewerUUIDs); }
        createWrapperEntityFromTRS(TextDisplayUtil.computeParallelogramTRS(p1, p2, p3));
        if (doubleSided) { createWrapperEntityFromTRS(TextDisplayUtil.computeParallelogramTRS(p1, p3, p2)); }
        spawned = true;
    }

    private void createWrapperEntityFromTRS(TRSResult trs) {
        Vector3f adj = new Vector3f(trs.translation()).sub((float) origin.getX(), (float) origin.getY(), (float) origin.getZ());
        WrapperEntity entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(origin));
        if (entity.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setText(net.kyori.adventure.text.Component.text(" "));
            meta.setBackgroundColor(argbColor);
            meta.setSeeThrough(seeThrough);
            if (entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                displayMeta.setBrightnessOverride(blockLight << 4 | skyLight << 20);
                displayMeta.setViewRange(viewRange);
                displayMeta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(adj.x, adj.y, adj.z));
                displayMeta.setScale(new com.github.retrooper.packetevents.util.Vector3f(trs.scale().x, trs.scale().y, trs.scale().z));
                displayMeta.setLeftRotation(new com.github.retrooper.packetevents.util.Quaternion4f(trs.leftRotation().x, trs.leftRotation().y, trs.leftRotation().z, trs.leftRotation().w));
                displayMeta.setRightRotation(new com.github.retrooper.packetevents.util.Quaternion4f(trs.rightRotation().x, trs.rightRotation().y, trs.rightRotation().z, trs.rightRotation().w));
            }
        }
        for (UUID uuid : viewerUUIDs) { entity.addViewer(uuid); }
        entities.add(entity);
        if (rootAnchorEnabled) { PacketRootAnchorSupport.attachPassenger(rootAnchor, entity); }
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

    public static class Builder implements ShapeBuilder<PacketParallelogram> {
        private final Location origin;
        private final Vector3f p1, p2, p3;
        private int argbColor = Color.fromARGB(150, 100, 50, 150).asARGB();
        private boolean doubleSided = false;
        private int blockLight = 15, skyLight = 15;
        private boolean seeThrough = true;
        private float viewRange = 1.0f;
        private boolean rootAnchorEnabled = false;

        public Builder(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) { this.origin = origin; this.p1 = p1; this.p2 = p2; this.p3 = p3; }
        public Builder color(Color color) { this.argbColor = color.asARGB(); return this; }
        @Override public Builder color(int argb) { this.argbColor = argb; return this; }
        @Override public Builder doubleSided(boolean v) { this.doubleSided = v; return this; }
        @Override public Builder brightness(int b, int s) { this.blockLight = b; this.skyLight = s; return this; }
        @Override public Builder seeThrough(boolean v) { this.seeThrough = v; return this; }
        @Override public Builder viewRange(float v) { this.viewRange = v; return this; }
        @Override public Builder rootAnchor(boolean v) { this.rootAnchorEnabled = v; return this; }
        @Override public PacketParallelogram build() { return new PacketParallelogram(this); }
    }
}
