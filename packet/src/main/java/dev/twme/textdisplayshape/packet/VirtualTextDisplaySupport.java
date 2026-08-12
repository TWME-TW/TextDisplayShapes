package dev.twme.textdisplayshape.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.github.twme.virtualentities.VirtualEntity;
import io.github.twme.virtualentities.VirtualEntityManager;
import io.github.twme.virtualentities.metadata.EntityMetadataFlags;
import io.github.twme.virtualentities.metadata.EntityMetadataKeys;
import io.github.twme.virtualentities.metadata.GeneratedEntityMetadataKeys;
import io.github.twme.virtualentities.metadata.MetadataKey;
import io.github.twme.virtualentities.metadata.VirtualMetadata;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Matrix4f;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

final class VirtualTextDisplaySupport {
    private static final MetadataKey<Integer> LEGACY_INTERPOLATION_START_DELTA_TICKS = MetadataKey.of(
            "INTERPOLATION_START_DELTA_TICKS",
            EntityDataTypes.INT
    );
    private static final MetadataKey<Integer> LEGACY_INTERPOLATION_DURATION = MetadataKey.of(
            "INTERPOLATION_DURATION",
            EntityDataTypes.INT
    );

    private VirtualTextDisplaySupport() {
    }

    static VirtualEntity createTextDisplay(
            VirtualEntityManager manager,
            Location origin,
            Set<UUID> viewerUUIDs,
            Consumer<VirtualEntity> configure
    ) {
        VirtualEntity entity = Objects.requireNonNull(manager, "manager")
                .entity(EntityTypes.TEXT_DISPLAY)
                .metadata()
                .build();
        try {
            configure.accept(entity);
        } catch (RuntimeException | Error exception) {
            entity.remove();
            throw exception;
        }
        addViewers(entity, viewerUUIDs);
        entity.spawn(SpigotConversionUtil.fromBukkitLocation(origin));
        return entity;
    }

    static void configureTextDisplay(
            VirtualEntity entity,
            int argbColor,
            boolean seeThrough,
            int blockLight,
            int skyLight,
            float viewRange
    ) {
        entity.metadata()
                .set(GeneratedEntityMetadataKeys.TextDisplay.TEXT, Component.text(" "))
                .set(GeneratedEntityMetadataKeys.TextDisplay.BACKGROUND_COLOR, argbColor)
                .setFlag(EntityMetadataFlags.TextDisplay.SEE_THROUGH, seeThrough)
                .set(GeneratedEntityMetadataKeys.Display.BRIGHTNESS_OVERRIDE, packBrightness(blockLight, skyLight))
                .set(GeneratedEntityMetadataKeys.Display.VIEW_RANGE, viewRange);
    }

    static void setTransformFromMatrix(VirtualEntity entity, Matrix4f matrix) {
        org.joml.Vector3f translation = new org.joml.Vector3f();
        matrix.getTranslation(translation);
        org.joml.Vector3f scale = new org.joml.Vector3f();
        matrix.getScale(scale);
        org.joml.Quaternionf rotation = new org.joml.Quaternionf();
        matrix.getUnnormalizedRotation(rotation);
        setTransform(
                entity,
                translation,
                scale,
                rotation,
                null
        );
    }

    static void setTransform(
            VirtualEntity entity,
            org.joml.Vector3f translation,
            org.joml.Vector3f scale,
            org.joml.Quaternionf leftRotation,
            org.joml.Quaternionf rightRotation
    ) {
        entity.metadata()
                .set(GeneratedEntityMetadataKeys.Display.TRANSLATION, vector(translation))
                .set(GeneratedEntityMetadataKeys.Display.SCALE, vector(scale))
                .set(GeneratedEntityMetadataKeys.Display.LEFT_ROTATION, quaternion(leftRotation));
        if (rightRotation != null) {
            entity.metadata().set(GeneratedEntityMetadataKeys.Display.RIGHT_ROTATION, quaternion(rightRotation));
        }
    }

    static void setImmediateInterpolation(VirtualEntity entity) {
        VirtualMetadata metadata = entity.metadata();
        setIfSupported(metadata, LEGACY_INTERPOLATION_START_DELTA_TICKS, 0);
        setIfSupported(metadata, LEGACY_INTERPOLATION_DURATION, 0);
        setIfSupported(
                metadata,
                GeneratedEntityMetadataKeys.Display.TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS,
                0
        );
        setIfSupported(metadata, GeneratedEntityMetadataKeys.Display.TRANSFORMATION_INTERPOLATION_DURATION, 0);
        setIfSupported(metadata, GeneratedEntityMetadataKeys.Display.POS_ROT_INTERPOLATION_DURATION, 0);
    }

    static void addViewers(VirtualEntity entity, Collection<UUID> viewerUUIDs) {
        for (UUID viewerUUID : viewerUUIDs) {
            addViewer(entity, viewerUUID);
        }
    }

    static void addViewer(VirtualEntity entity, UUID viewerUUID) {
        Player player = Bukkit.getPlayer(viewerUUID);
        if (player == null || !player.isOnline()) {
            return;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user != null) {
            entity.addViewer(user);
        }
    }

    static void rebaseEntities(
            VirtualEntityManager manager,
            Collection<VirtualEntity> entities,
            Location oldOrigin,
            Location newOrigin
    ) {
        float deltaX = (float) (newOrigin.getX() - oldOrigin.getX());
        float deltaY = (float) (newOrigin.getY() - oldOrigin.getY());
        float deltaZ = (float) (newOrigin.getZ() - oldOrigin.getZ());
        manager.bundle(() -> {
            for (VirtualEntity entity : entities) {
                shiftTranslation(entity, deltaX, deltaY, deltaZ);
                entity.teleport(SpigotConversionUtil.fromBukkitLocation(newOrigin));
            }
        });
    }

    static void rebaseRootAnchor(
            VirtualEntityManager manager,
            VirtualEntity rootAnchor,
            Collection<VirtualEntity> childEntities,
            Location oldOrigin,
            Location newOrigin
    ) {
        float deltaX = (float) (newOrigin.getX() - oldOrigin.getX());
        float deltaY = (float) (newOrigin.getY() - oldOrigin.getY());
        float deltaZ = (float) (newOrigin.getZ() - oldOrigin.getZ());
        manager.bundle(() -> {
            for (VirtualEntity entity : childEntities) {
                shiftTranslation(entity, deltaX, deltaY, deltaZ);
            }
            rootAnchor.teleport(SpigotConversionUtil.fromBukkitLocation(newOrigin));
        });
    }

    static int packBrightness(int blockLight, int skyLight) {
        if (blockLight < 0 || blockLight > 15 || skyLight < 0 || skyLight > 15) {
            throw new IllegalArgumentException("Light levels must be between 0 and 15");
        }
        return blockLight << 4 | skyLight << 20;
    }

    private static void shiftTranslation(VirtualEntity entity, float deltaX, float deltaY, float deltaZ) {
        entity.metadata().get(GeneratedEntityMetadataKeys.Display.TRANSLATION).ifPresent(oldTranslation -> {
            setImmediateInterpolation(entity);
            entity.metadata().set(
                    GeneratedEntityMetadataKeys.Display.TRANSLATION,
                    new Vector3f(
                            oldTranslation.getX() - deltaX,
                            oldTranslation.getY() - deltaY,
                            oldTranslation.getZ() - deltaZ
                    )
            );
            entity.syncMetadata();
        });
    }

    private static Vector3f vector(org.joml.Vector3f value) {
        return new Vector3f(value.x, value.y, value.z);
    }

    private static <T> void setIfSupported(VirtualMetadata metadata, MetadataKey<T> key, T value) {
        if (metadata.schema().find(key.fieldName()).isPresent()) {
            metadata.set(key, value);
        }
    }

    private static Quaternion4f quaternion(org.joml.Quaternionf value) {
        return new Quaternion4f(value.x, value.y, value.z, value.w);
    }
}
