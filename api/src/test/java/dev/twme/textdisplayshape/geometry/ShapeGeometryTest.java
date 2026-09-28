package dev.twme.textdisplayshape.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.twme.textdisplayshape.util.TextDisplayUtil;

class ShapeGeometryTest {

    @Test
    void entityCountsMatchEachShape() {
        Vector3f a = new Vector3f(0, 0, 0);
        Vector3f b = new Vector3f(2, 0, 0);
        Vector3f c = new Vector3f(0, 2, 0);
        assertEquals(1, ShapeGeometry.line(a, b, 0.1f, 0f, false).size());
        assertEquals(2, ShapeGeometry.line(a, b, 0.1f, 0f, true).size());
        assertEquals(3, ShapeGeometry.triangle(a, b, c, false).size());
        assertEquals(6, ShapeGeometry.triangle(a, b, c, true).size());
        assertEquals(1, ShapeGeometry.parallelogram(a, b, c, false).size());
        assertEquals(2, ShapeGeometry.parallelogram(a, b, c, true).size());
        assertEquals(2, ShapeGeometry.polyline(List.of(a, b, c), 0.1f, 0f, false, false).size());
        assertEquals(3, ShapeGeometry.polyline(List.of(a, b, c), 0.1f, 0f, true, false).size());
        assertEquals(1, ShapeGeometry.polyline(List.of(a, b), 0.1f, 0f, true, false).size(),
                "two points cannot form a closing segment");
        assertEquals(0, ShapeGeometry.polyline(List.of(a), 0.1f, 0f, false, false).size());
    }

    @Test
    void lineTransformReproducesTheLineMatrix() {
        Vector3f from = new Vector3f(1.5f, 2f, -3f);
        Vector3f to = new Vector3f(4f, 5.5f, 1f);
        Matrix4f expected = TextDisplayUtil.textDisplayLine(from, to, 0.2f, 0.3f);
        Matrix4f actual = ShapeGeometry.line(from, to, 0.2f, 0.3f, false).get(0).toMatrix();
        assertMatrixEquals(expected, actual);
    }

    @Test
    void relativeToOnlyShiftsTranslation() {
        DisplayTransform transform = ShapeGeometry.parallelogram(
                new Vector3f(10, 20, 30), new Vector3f(11, 20, 30), new Vector3f(10, 21, 30), false).get(0);
        DisplayTransform relative = transform.relativeTo(10, 20, 30);
        assertTrue(relative.translation().distance(transform.translation().sub(10, 20, 30)) < 1e-5f);
        assertEquals(transform.scale(), relative.scale());
        assertEquals(transform.leftRotation(), relative.leftRotation());
    }

    @Test
    void invalidGeometryIsRejected() {
        Vector3f point = new Vector3f(1, 1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> ShapeGeometry.line(point, new Vector3f(point), 0.1f, 0f, false));
    }

    @Test
    void boxEdgesAndOutwardFaces() {
        Vector3f min = new Vector3f(0, 0, 0);
        Vector3f max = new Vector3f(1, 2, 3);
        List<BoxGeometry.Edge> edges = BoxGeometry.edges(max, min);
        assertEquals(12, edges.size());
        float total = 0;
        for (BoxGeometry.Edge edge : edges) {
            total += edge.from().distance(edge.to());
        }
        assertEquals(4 * (1 + 2 + 3), total, 1e-5f);

        Vector3f center = new Vector3f(0.5f, 1f, 1.5f);
        List<BoxGeometry.Face> faces = BoxGeometry.faces(min, max);
        assertEquals(6, faces.size());
        for (BoxGeometry.Face face : faces) {
            Vector3f normal = new Vector3f(face.first()).sub(face.corner())
                    .cross(new Vector3f(face.second()).sub(face.corner()));
            Vector3f outward = new Vector3f(face.corner()).sub(center);
            assertTrue(normal.dot(outward) > 0, "face must point outward: " + face);
        }
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        float[] e = expected.get(new float[16]);
        float[] a = actual.get(new float[16]);
        for (int index = 0; index < 16; index++) {
            assertEquals(e[index], a[index], 1e-4f, "matrix element " + index);
        }
    }
}
