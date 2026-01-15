package dev.twme.textdisplayshape.bukkit;

import org.bukkit.Location;
import org.joml.Vector3f;

import java.util.List;

/**
 * Factory class for Bukkit-based shapes.
 * Uses direct entity manipulation to display shapes.
 */
public class BukkitShapeFactory {

    /**
     * Creates a triangle builder.
     *
     * @param origin the spawn location (usually the player's location)
     * @param p1     the first vertex (world coordinates)
     * @param p2     the second vertex (world coordinates)
     * @param p3     the third vertex (world coordinates)
     * @return the triangle builder
     */
    public BukkitTriangle.Builder triangle(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new BukkitTriangle.Builder(origin, p1, p2, p3);
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
    public BukkitLine.Builder line(Location origin, Vector3f p1, Vector3f p2, float thickness) {
        return new BukkitLine.Builder(origin, p1, p2, thickness);
    }

    /**
     * Creates a polyline (connected line segments) builder.
     *
     * @param origin    the spawn location (usually the player's location)
     * @param points    the list of points defining the polyline (at least 2
     *                  required)
     * @param thickness the line thickness
     * @return the polyline builder
     */
    public BukkitPolyline.Builder polyline(Location origin, List<Vector3f> points, float thickness) {
        return new BukkitPolyline.Builder(origin, points, thickness);
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
    public BukkitParallelogram.Builder parallelogram(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new BukkitParallelogram.Builder(origin, p1, p2, p3);
    }
}
