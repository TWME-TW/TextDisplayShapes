package dev.twme.textdisplayshape.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class TextDisplayUtilTest {

    private static final float EPSILON = 1.0E-4F;

    @Test
    void parallelogramTrsReconstructsTransformationMatrix() {
        List<Vector3f[]> cases = List.of(
                points(2.5f, -1.25f, 4f, 7f, 0.5f, 2f, 3.5f, 3f, 6f),
                points(0f, 0f, 0f, 4f, 0f, 0f, 1f, 3f, 0f),
                points(-2f, 5f, 1f, -2f, 5f, 6f, -4f, 1f, 3f));

        for (Vector3f[] vertices : cases) {
            Matrix4f expected = TextDisplayUtil.textDisplayParallelogram(
                    vertices[0], vertices[1], vertices[2]);
            Matrix4f actual = reconstruct(TextDisplayUtil.computeParallelogramTRS(
                    vertices[0], vertices[1], vertices[2]));

            assertMatrixEquals(expected, actual);
        }
    }

    @Test
    void triangleTrsReconstructsEveryTransformationMatrix() {
        List<Vector3f[]> cases = List.of(
                points(-3f, 2f, 1f, 2f, 3f, -1f, -1f, 7f, 4f),
                points(0f, 0f, 0f, 4f, 0f, 0f, 1f, 3f, 0f),
                points(0f, 0f, 0f, 1f, 3f, 0f, 4f, 0f, 0f),
                points(-2f, 5f, 1f, -2f, 5f, 6f, -4f, 1f, 3f));

        for (Vector3f[] vertices : cases) {
            List<Matrix4f> expected = TextDisplayUtil.textDisplayTriangle(
                    vertices[0], vertices[1], vertices[2]).transforms;
            List<TRSResult> actual = TextDisplayUtil.computeTriangleTRS(
                    vertices[0], vertices[1], vertices[2]);

            assertEquals(expected.size(), actual.size());
            for (int i = 0; i < expected.size(); i++) {
                assertMatrixEquals(expected.get(i), reconstruct(actual.get(i)));
            }
        }
    }

    @Test
    void lineTransformationContainsOnlyFiniteValues() {
        Matrix4f matrix = TextDisplayUtil.textDisplayLine(
                new Vector3f(1f, 2f, 3f), new Vector3f(1f, 8f, 3f), 0.25f, (float) Math.PI / 3f);

        assertTrue(matrix.isFinite());
    }

    @Test
    void rejectsInvalidLineGeometry() {
        Vector3f point = new Vector3f(1f, 2f, 3f);

        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.textDisplayLine(point, point, 0.1f));
        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.textDisplayLine(point, new Vector3f(2f, 2f, 3f), 0f));
        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.textDisplayLine(point, new Vector3f(Float.NaN, 2f, 3f), 0.1f));
    }

    @Test
    void rejectsDegenerateSurfaceGeometry() {
        Vector3f p1 = new Vector3f(0f, 0f, 0f);
        Vector3f p2 = new Vector3f(1f, 1f, 1f);
        Vector3f p3 = new Vector3f(2f, 2f, 2f);

        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.textDisplayTriangle(p1, p2, p3));
        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.computeTriangleTRS(p1, p1, p3));
        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.textDisplayParallelogram(p1, p2, p3));
        assertThrows(IllegalArgumentException.class,
                () -> TextDisplayUtil.computeParallelogramTRS(p1, p2, p3));
    }

    private static Matrix4f reconstruct(TRSResult trs) {
        return new Matrix4f()
                .translation(trs.translation())
                .rotate(trs.leftRotation())
                .scale(trs.scale())
                .rotate(trs.rightRotation());
    }

    private static Vector3f[] points(float... coordinates) {
        return new Vector3f[] {
                new Vector3f(coordinates[0], coordinates[1], coordinates[2]),
                new Vector3f(coordinates[3], coordinates[4], coordinates[5]),
                new Vector3f(coordinates[6], coordinates[7], coordinates[8])
        };
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual) {
        assertTrue(expected.equals(actual, EPSILON), () -> "expected:\n" + expected + "\nbut was:\n" + actual);
    }
}
