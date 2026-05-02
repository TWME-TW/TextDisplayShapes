package dev.twme.textdisplayshape.shape;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base interface for all shapes.
 * Defines lifecycle management and viewer management methods.
 *
 * <p>This interface is platform-agnostic and does not depend on any
 * specific server implementation (Bukkit, Fabric, Minestom, etc.).</p>
 */
public interface Shape {

    /**
     * Spawns the shape into the world.
     */
    void spawn();

    /**
     * Removes the shape from the world.
     */
    void remove();

    /**
     * Checks if the shape has been spawned.
     *
     * @return true if spawned
     */
    boolean isSpawned();

    /**
     * Adds a viewer by UUID who can see this shape (only applicable in packet mode).
     * <p>
     * Using UUID instead of platform-specific player objects avoids memory leaks
     * caused by player object invalidation (e.g., Bukkit recreates Player objects
     * on respawn, changing their internal hash codes).
     *
     * @param playerUUID the UUID of the player to add as viewer
     */
    void addViewer(UUID playerUUID);

    /**
     * Removes a viewer by UUID from seeing this shape (only applicable in packet mode).
     *
     * @param playerUUID the UUID of the player to remove from viewers
     */
    void removeViewer(UUID playerUUID);

    /**
     * Gets the UUIDs of all players who can see this shape.
     *
     * @return set of viewer player UUIDs
     */
    Set<UUID> getViewerUUIDs();

    /**
     * Gets the UUIDs of all entities that make up this shape.
     * A shape may consist of multiple TextDisplay entities (e.g., a triangle
     * consists of 3 entities).
     *
     * @return list of entity UUIDs, empty list if not yet spawned
     */
    List<UUID> getEntityUUIDs();

    /**
     * Teleports all entities to a new origin and adjusts their translation offsets
     * so that the shape remains at the same absolute world position.
     * <p>
     * This is useful when the player moves far from the original spawn point,
     * causing TextDisplay entities to exceed their view range and become invisible.
     * <p>
     * The position is represented as (x, y, z) coordinates. Platform-specific
     * implementations may accept additional world/dimension information.
     *
     * @param x the new origin X coordinate
     * @param y the new origin Y coordinate
     * @param z the new origin Z coordinate
     */
    void teleportOrigin(double x, double y, double z);
}
