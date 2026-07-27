package io.github.twme.textdisplayshapes.integration;

import dev.twme.textdisplayshape.packet.PacketLine;
import dev.twme.textdisplayshape.packet.PacketShapeFactory;
import io.github.twme.virtualentities.VirtualEntities;
import io.github.twme.virtualentities.VirtualEntityManager;
import org.bukkit.Color;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Paper fixture that exercises the public TextDisplayShapes packet API. */
public final class TextDisplayShapesIntegrationPlugin extends JavaPlugin {
    private final Map<UUID, PacketLine> lines = new LinkedHashMap<>();
    private VirtualEntityManager entityManager;
    private PacketShapeFactory shapeFactory;

    @Override
    public void onEnable() {
        entityManager = VirtualEntities.create();
        shapeFactory = new PacketShapeFactory(entityManager);
    }

    @Override
    public void onDisable() {
        lines.values().forEach(PacketLine::remove);
        lines.clear();
        if (entityManager != null) {
            entityManager.close();
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] arguments
    ) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        PacketLine previous = lines.remove(player.getUniqueId());
        if (previous != null) {
            previous.remove();
        }

        org.bukkit.Location origin = player.getLocation();
        origin.setYaw(0);
        origin.setPitch(0);
        PacketLine line = shapeFactory.line(
                        origin,
                        new Vector3f((float) origin.getX() + 2, (float) origin.getY(), (float) origin.getZ()),
                        new Vector3f((float) origin.getX() + 4, (float) origin.getY(), (float) origin.getZ()),
                        0.1f
                )
                .color(Color.fromARGB(200, 255, 100, 100))
                .brightness(15, 15)
                .seeThrough(true)
                .viewRange(64.0f)
                .rootAnchor(true)
                .build();
        line.addViewer(player.getUniqueId());
        line.spawn();
        lines.put(player.getUniqueId(), line);
        player.sendMessage("TDS_READY:" + line.getEntities().get(0).entityId());

        getServer().getScheduler().runTaskLater(this, () -> {
            if (line.isSpawned()) {
                line.teleportOrigin(origin.getX() + 1, origin.getY(), origin.getZ());
                player.sendMessage("TDS_RELOCATED");
            }
        }, 20L);
        return true;
    }
}
