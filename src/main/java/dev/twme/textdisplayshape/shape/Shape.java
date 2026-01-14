package dev.twme.textdisplayshape.shape;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Base interface for all shapes.
 * Defines lifecycle management and viewer management methods.
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
     * Adds a player who can see this shape (only applicable in packet mode).
     *
     * @param player the player to add as viewer
     */
    void addViewer(Player player);

    /**
     * Removes a player from seeing this shape (only applicable in packet mode).
     *
     * @param player the player to remove from viewers
     */
    void removeViewer(Player player);

    /**
     * Gets all players who can see this shape.
     *
     * @return set of viewer players
     */
    Set<Player> getViewers();

    /**
     * Gets the UUIDs of all entities that make up this shape.
     * A shape may consist of multiple TextDisplay entities (e.g., a triangle
     * consists of 3 entities).
     *
     * @return list of entity UUIDs, empty list if not yet spawned
     */
    List<UUID> getEntityUUIDs();
}
