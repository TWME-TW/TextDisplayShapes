package dev.twme.textdisplayshape.packet;

import org.bukkit.Location;
import org.joml.Vector3f;

/**
 * Factory class for packet-based shapes.
 * Uses EntityLib packet-based approach to display shapes. EntityLib must be
 * initialized first.
 */
public class PacketShapeFactory {

    /**
     * Creates a triangle builder.
     *
     * @param origin the spawn location (usually the player's location)
     * @param p1     the first vertex (world coordinates)
     * @param p2     the second vertex (world coordinates)
     * @param p3     the third vertex (world coordinates)
     * @return the triangle builder
     */
    public PacketTriangle.Builder triangle(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new PacketTriangle.Builder(origin, p1, p2, p3);
    }

    /**
     * Creates a line builder.
     *
     * @param origin    the spawn location (usually the player's location)
     * @param p1        the start point (world coordinates)
     * @param p2        the end point (world coordinates)
     * @param thickness the line thickness
     * @return the line builder
     */
    public PacketLine.Builder line(Location origin, Vector3f p1, Vector3f p2, float thickness) {
        return new PacketLine.Builder(origin, p1, p2, thickness);
    }

    /**
     * Creates a parallelogram builder.
     *
     * @param origin the spawn location (usually the player's location)
     * @param p1     the starting corner (world coordinates)
     * @param p2     the second point, defines the width direction (world
     *               coordinates)
     * @param p3     the third point, defines the height direction (world
     *               coordinates)
     * @return the parallelogram builder
     */
    public PacketParallelogram.Builder parallelogram(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new PacketParallelogram.Builder(origin, p1, p2, p3);
    }
}
