package dev.twme.textdisplayshape.packet;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;

import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;

final class PacketRootAnchorSupport {

    private static final float ROOT_SCALE = 0.0001f;

    private PacketRootAnchorSupport() {
    }

    static Location toAnchorLocation(Location logicalOrigin) {
        return logicalOrigin.clone();
    }

    static WrapperEntity createRootAnchor(Location origin, float viewRange, Set<UUID> viewerUUIDs) {
        WrapperEntity rootAnchor = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        rootAnchor.spawn(SpigotConversionUtil.fromBukkitLocation(toAnchorLocation(origin)));

        if (rootAnchor.getEntityMeta() instanceof TextDisplayMeta meta) {
            meta.setText(Component.empty());
            meta.setBackgroundColor(0);
            meta.setSeeThrough(true);

            if (rootAnchor.getEntityMeta() instanceof AbstractDisplayMeta displayMeta) {
                displayMeta.setViewRange(viewRange);
                displayMeta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(0f, 0f, 0f));
                displayMeta.setScale(new com.github.retrooper.packetevents.util.Vector3f(ROOT_SCALE, ROOT_SCALE, ROOT_SCALE));
                displayMeta.setInterpolationDelay(0);
                displayMeta.setTransformationInterpolationDuration(0);
                displayMeta.setPositionRotationInterpolationDuration(0);
            }
        }

        rootAnchor.setHasNoGravity(true);

        for (UUID viewerUUID : viewerUUIDs) {
            rootAnchor.addViewer(viewerUUID);
        }

        return rootAnchor;
    }

    static void attachPassenger(WrapperEntity rootAnchor, WrapperEntity childEntity) {
        if (rootAnchor == null) return;
        rootAnchor.addPassenger(childEntity.getEntityId());
    }

    static void teleportRootAnchor(WrapperEntity rootAnchor, java.util.List<WrapperEntity> childEntities,
                                   Location oldOrigin, Location newOrigin) {
        if (rootAnchor == null) return;

        float deltaX = (float) (newOrigin.getX() - oldOrigin.getX());
        float deltaY = (float) (newOrigin.getY() - oldOrigin.getY());
        float deltaZ = (float) (newOrigin.getZ() - oldOrigin.getZ());

        com.github.retrooper.packetevents.protocol.world.Location anchorLocation =
                SpigotConversionUtil.fromBukkitLocation(toAnchorLocation(newOrigin));

        rootAnchor.sendPacketToViewers(new WrapperPlayServerBundle());

        for (WrapperEntity entity : childEntities) {
            if (!(entity.getEntityMeta() instanceof AbstractDisplayMeta displayMeta)) {
                continue;
            }

            com.github.retrooper.packetevents.util.Vector3f old = displayMeta.getTranslation();
            displayMeta.setInterpolationDelay(0);
            displayMeta.setTransformationInterpolationDuration(0);
            displayMeta.setPositionRotationInterpolationDuration(0);
            displayMeta.setTranslation(new com.github.retrooper.packetevents.util.Vector3f(
                    old.getX() - deltaX,
                    old.getY() - deltaY,
                    old.getZ() - deltaZ));

            entity.sendPacketToViewers(entity.getEntityMeta().createPacket());
        }

        rootAnchor.sendPacketToViewers(new WrapperPlayServerEntityTeleport(
                rootAnchor.getEntityId(),
                anchorLocation.getPosition(),
                anchorLocation.getYaw(),
                anchorLocation.getPitch(),
                false));
        rootAnchor.sendPacketToViewers(new WrapperPlayServerBundle());
        rootAnchor.setLocation(anchorLocation);
    }
}