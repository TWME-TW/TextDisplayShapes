package dev.twme.textdisplayshape.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import io.github.twme.virtualentities.VirtualEntityManager;
import io.github.twme.virtualentities.VirtualViewer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

final class PacketViewerRegistry {
    private static final Map<VirtualEntityManager, PacketViewerRegistry> REGISTRIES = new WeakHashMap<>();

    private final ConcurrentMap<UUID, CachedViewer> viewers = new ConcurrentHashMap<>();

    PacketViewerRegistry() {
    }

    static PacketViewerRegistry forManager(VirtualEntityManager manager) {
        Objects.requireNonNull(manager, "manager");
        synchronized (REGISTRIES) {
            return REGISTRIES.computeIfAbsent(manager, ignored -> new PacketViewerRegistry());
        }
    }

    VirtualViewer viewer(UUID viewerUUID) {
        Player player = Bukkit.getPlayer(viewerUUID);
        if (player == null || !player.isOnline()) {
            return null;
        }
        return viewer(PacketEvents.getAPI().getPlayerManager().getUser(player));
    }

    VirtualViewer viewer(User user) {
        if (user == null || user.getUUID() == null) {
            return null;
        }
        return viewer(user.getUUID(), user, () -> VirtualViewer.of(user));
    }

    VirtualViewer viewer(UUID viewerId, Object connectionIdentity, Supplier<VirtualViewer> viewerFactory) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(connectionIdentity, "connectionIdentity");
        Objects.requireNonNull(viewerFactory, "viewerFactory");
        AtomicReference<VirtualViewer> selected = new AtomicReference<>();
        viewers.compute(viewerId, (ignored, cached) -> {
            VirtualViewer currentViewer = cached == null ? null : cached.viewer().get();
            if (currentViewer != null && cached.connectionIdentity().get() == connectionIdentity) {
                selected.set(currentViewer);
                return cached;
            }
            VirtualViewer replacement = Objects.requireNonNull(viewerFactory.get(), "viewerFactory result");
            selected.set(replacement);
            return new CachedViewer(
                    new WeakReference<>(connectionIdentity),
                    new WeakReference<>(replacement)
            );
        });
        return selected.get();
    }

    private record CachedViewer(
            WeakReference<Object> connectionIdentity,
            WeakReference<VirtualViewer> viewer
    ) {
    }
}
