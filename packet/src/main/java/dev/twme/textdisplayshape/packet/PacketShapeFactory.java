package dev.twme.textdisplayshape.packet;

import org.bukkit.Location;
import org.joml.Vector3f;

import java.util.List;

/**
 * Factory class for packet-based shapes.
 * Uses EntityLib packet-based approach to display shapes. EntityLib must be
 * initialized first.
 */
public class PacketShapeFactory {

    public PacketTriangle.Builder triangle(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new PacketTriangle.Builder(origin, p1, p2, p3);
    }

    public PacketLine.Builder line(Location origin, Vector3f p1, Vector3f p2, float thickness) {
        return new PacketLine.Builder(origin, p1, p2, thickness);
    }

    public PacketPolyline.Builder polyline(Location origin, List<Vector3f> points, float thickness) {
        return new PacketPolyline.Builder(origin, points, thickness);
    }

    public PacketParallelogram.Builder parallelogram(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
        return new PacketParallelogram.Builder(origin, p1, p2, p3);
    }
}
