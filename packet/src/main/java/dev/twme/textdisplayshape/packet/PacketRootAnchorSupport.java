package dev.twme.textdisplayshape.packet;

import com.github.retrooper.packetevents.util.Vector3f;
import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;
import io.github.twme.virtualentities.metadata.EntityMetadataFlags;
import io.github.twme.virtualentities.metadata.EntityMetadataKeys;
import io.github.twme.virtualentities.metadata.GeneratedEntityMetadataKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

import java.util.List;
import java.util.Set;
import java.util.UUID;

final class PacketRootAnchorSupport {
    private static final float ROOT_SCALE = 0.0001f;

    private PacketRootAnchorSupport() {
    }

    static Location toAnchorLocation(Location logicalOrigin) {
        return logicalOrigin.clone();
    }

    static VirtualEntity createRootAnchor(
            VirtualEntityManager manager,
            Location origin,
            float viewRange,
            Set<UUID> viewerUUIDs
    ) {
        return VirtualTextDisplaySupport.createTextDisplay(
                manager,
                toAnchorLocation(origin),
                viewerUUIDs,
                rootAnchor -> {
                    rootAnchor.metadata()
                            .set(GeneratedEntityMetadataKeys.TextDisplay.TEXT, Component.empty())
                            .set(GeneratedEntityMetadataKeys.TextDisplay.BACKGROUND_COLOR, 0)
                            .setFlag(EntityMetadataFlags.TextDisplay.SEE_THROUGH, true)
                            .set(EntityMetadataKeys.NO_GRAVITY, true)
                            .set(GeneratedEntityMetadataKeys.Display.VIEW_RANGE, viewRange)
                            .set(GeneratedEntityMetadataKeys.Display.TRANSLATION, new Vector3f(0f, 0f, 0f))
                            .set(GeneratedEntityMetadataKeys.Display.SCALE, new Vector3f(ROOT_SCALE, ROOT_SCALE, ROOT_SCALE));
                    VirtualTextDisplaySupport.setImmediateInterpolation(rootAnchor);
                }
        );
    }

    static void attachPassenger(VirtualEntity rootAnchor, VirtualEntity childEntity) {
        if (rootAnchor != null) {
            rootAnchor.addPassenger(childEntity);
        }
    }

    static void teleportRootAnchor(
            VirtualEntityManager manager,
            VirtualEntity rootAnchor,
            List<VirtualEntity> childEntities,
            Location oldOrigin,
            Location newOrigin
    ) {
        if (rootAnchor != null) {
            VirtualTextDisplaySupport.rebaseRootAnchor(manager, rootAnchor, childEntities, oldOrigin, newOrigin);
        }
    }
}
