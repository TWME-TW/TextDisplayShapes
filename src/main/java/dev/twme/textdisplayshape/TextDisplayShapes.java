package dev.twme.textdisplayshape;

import dev.twme.textdisplayshape.bukkit.BukkitShapeFactory;
import dev.twme.textdisplayshape.packet.PacketShapeFactory;

/**
 * Main entry point for the TextDisplayShapes library.
 * Provides two ways to display geometric shapes:
 * <ul>
 * <li>{@link #bukkit()} - Uses Bukkit API to directly manipulate entities</li>
 * <li>{@link #packet()} - Uses EntityLib to display shapes via packets</li>
 * </ul>
 *
 * <h2>Usage Example (Bukkit)</h2>
 * 
 * <pre>{@code
 * // Create a triangle
 * Shape triangle = TextDisplayShapes.bukkit()
 *         .triangle(playerLocation, p1, p2, p3)
 *         .color(Color.fromARGB(150, 50, 100, 100))
 *         .doubleSided(true)
 *         .build();
 * triangle.spawn();
 *
 * // Remove the triangle
 * triangle.remove();
 * }</pre>
 *
 * <h2>Usage Example (Packet/EntityLib)</h2>
 * 
 * <pre>{@code
 * // EntityLib must be initialized first
 * // EntityLib.init(platform, settings);
 *
 * // Create a line
 * Shape line = TextDisplayShapes.packet()
 *         .line(playerLocation, p1, p2, 0.1f)
 *         .color(Color.RED)
 *         .build();
 *
 * // Add viewer
 * line.addViewer(player);
 * line.spawn();
 *
 * // Remove the line
 * line.remove();
 * }</pre>
 */
public final class TextDisplayShapes {

    private static final BukkitShapeFactory BUKKIT_FACTORY = new BukkitShapeFactory();
    private static final PacketShapeFactory PACKET_FACTORY = new PacketShapeFactory();

    private TextDisplayShapes() {
        // Utility class, prevent instantiation
    }

    /**
     * Gets the Bukkit shape factory.
     * Shapes created with this factory will directly create TextDisplay entities on
     * the server,
     * visible to all players.
     *
     * @return the Bukkit shape factory
     */
    public static BukkitShapeFactory bukkit() {
        return BUKKIT_FACTORY;
    }

    /**
     * Gets the packet shape factory.
     * Shapes created with this factory will only be sent via packets to specified
     * viewers,
     * no actual entities are created on the server.
     *
     * <p>
     * <strong>Note:</strong> EntityLib must be initialized before use:
     * </p>
     * 
     * <pre>{@code
     * SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
     * APIConfig settings = new APIConfig(PacketEvents.getAPI())
     *         .tickTickables()
     *         .trackPlatformEntities();
     * EntityLib.init(platform, settings);
     * }</pre>
     *
     * @return the packet shape factory
     */
    public static PacketShapeFactory packet() {
        return PACKET_FACTORY;
    }
}
