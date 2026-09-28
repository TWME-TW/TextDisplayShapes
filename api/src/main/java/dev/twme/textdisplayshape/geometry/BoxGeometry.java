package dev.twme.textdisplayshape.geometry;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

/** Edges and faces of an axis-aligned box. */
public final class BoxGeometry {

    /** One of the twelve box edges. */
    public record Edge(Vector3f from, Vector3f to) {
        public Edge {
            from = new Vector3f(from);
            to = new Vector3f(to);
        }
    }

    /**
     * One of the six box faces as a parallelogram: {@code corner} is shared by
     * the two edges ending at {@code first} and {@code second}. The front side
     * ({@code (first - corner) x (second - corner)}) faces outward.
     */
    public record Face(Vector3f corner, Vector3f first, Vector3f second) {
        public Face {
            corner = new Vector3f(corner);
            first = new Vector3f(first);
            second = new Vector3f(second);
        }
    }

    private BoxGeometry() {
    }

    /** The eight corners, indexed by bits x=4, y=2, z=1. */
    public static List<Vector3f> corners(Vector3f min, Vector3f max) {
        Vector3f low = new Vector3f(min).min(max);
        Vector3f high = new Vector3f(min).max(max);
        List<Vector3f> corners = new ArrayList<>(8);
        for (int index = 0; index < 8; index++) {
            corners.add(new Vector3f(
                    (index & 4) != 0 ? high.x : low.x,
                    (index & 2) != 0 ? high.y : low.y,
                    (index & 1) != 0 ? high.z : low.z));
        }
        return corners;
    }

    /** The twelve edges in a stable order: four along X, four along Y, four along Z. */
    public static List<Edge> edges(Vector3f min, Vector3f max) {
        List<Vector3f> c = corners(min, max);
        List<Edge> edges = new ArrayList<>(12);
        int[][] pairs = {
            {0, 4}, {1, 5}, {2, 6}, {3, 7},
            {0, 2}, {1, 3}, {4, 6}, {5, 7},
            {0, 1}, {2, 3}, {4, 5}, {6, 7}
        };
        for (int[] pair : pairs) {
            edges.add(new Edge(c.get(pair[0]), c.get(pair[1])));
        }
        return edges;
    }

    /** The six faces with outward front sides: -X, +X, -Y, +Y, -Z, +Z. */
    public static List<Face> faces(Vector3f min, Vector3f max) {
        List<Vector3f> c = corners(min, max);
        List<Face> faces = new ArrayList<>(6);
        faces.add(new Face(c.get(0), c.get(1), c.get(2)));
        faces.add(new Face(c.get(4), c.get(6), c.get(5)));
        faces.add(new Face(c.get(0), c.get(4), c.get(1)));
        faces.add(new Face(c.get(2), c.get(3), c.get(6)));
        faces.add(new Face(c.get(0), c.get(2), c.get(4)));
        faces.add(new Face(c.get(1), c.get(5), c.get(3)));
        return faces;
    }
}
