package dev.twme.textdisplayshape.packet;

import io.github.twme.virtualentities.VirtualEntities;
import io.github.twme.virtualentities.VirtualEntityManager;

final class PacketEntityManagers {
    private static final VirtualEntityManager DEFAULT_MANAGER = VirtualEntities.create();

    private PacketEntityManagers() {
    }

    static VirtualEntityManager defaultManager() {
        return DEFAULT_MANAGER;
    }
}
