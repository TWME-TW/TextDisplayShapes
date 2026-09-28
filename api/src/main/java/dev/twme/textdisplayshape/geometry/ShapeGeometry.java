package dev.twme.textdisplayshape.geometry;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import dev.twme.textdisplayshape.util.TRSResult;
import dev.twme.textdisplayshape.util.TextDisplayUtil;

/**
 * Platform-neutral Text Display transforms for every built-in shape. All
 * renderers use these methods, so direct and packet shapes stay identical.
 */
public final class ShapeGeometry {

    private ShapeGeometry() {
    }

    /** One entity per face: the front, then the back when double-sided. */
    public static List<DisplayTransform> line(
            Vector3f p1, Vector3f p2, float thickness, float roll, boolean doubleSided) {
        List<DisplayTransform> transforms = new ArrayList<>(doubleSided ? 2 : 1);
        transforms.add(DisplayTransform.ofUnshearedMatrix(TextDisplayUtil.textDisplayLine(p1, p2, thickness, roll)));
        if (doubleSided) {
            transforms.add(DisplayTransform.ofUnshearedMatrix(
                    TextDisplayUtil.textDisplayLine(p2, p1, thickness, -roll)));
        }
        return transforms;
    }

    /** Segments in order, then the closing segment when {@code closed}. */
    public static List<DisplayTransform> polyline(
            List<Vector3f> points, float thickness, float roll, boolean closed, boolean doubleSided) {
        List<DisplayTransform> transforms = new ArrayList<>();
        if (points.size() < 2) {
            return transforms;
        }
        for (int index = 0; index < points.size() - 1; index++) {
            transforms.addAll(line(points.get(index), points.get(index + 1), thickness, roll, doubleSided));
        }
        if (closed && points.size() > 2) {
            transforms.addAll(line(points.get(points.size() - 1), points.get(0), thickness, roll, doubleSided));
        }
        return transforms;
    }

    /** Three entities per face. */
    public static List<DisplayTransform> triangle(
            Vector3f p1, Vector3f p2, Vector3f p3, boolean doubleSided) {
        List<DisplayTransform> transforms = new ArrayList<>(doubleSided ? 6 : 3);
        for (TRSResult result : TextDisplayUtil.computeTriangleTRS(p1, p2, p3)) {
            transforms.add(DisplayTransform.of(result));
        }
        if (doubleSided) {
            for (TRSResult result : TextDisplayUtil.computeTriangleTRS(p1, p3, p2)) {
                transforms.add(DisplayTransform.of(result));
            }
        }
        return transforms;
    }

    /** One entity per face. */
    public static List<DisplayTransform> parallelogram(
            Vector3f p1, Vector3f p2, Vector3f p3, boolean doubleSided) {
        List<DisplayTransform> transforms = new ArrayList<>(doubleSided ? 2 : 1);
        transforms.add(DisplayTransform.of(TextDisplayUtil.computeParallelogramTRS(p1, p2, p3)));
        if (doubleSided) {
            transforms.add(DisplayTransform.of(TextDisplayUtil.computeParallelogramTRS(p1, p3, p2)));
        }
        return transforms;
    }
}
