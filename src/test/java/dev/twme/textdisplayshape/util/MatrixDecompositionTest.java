package dev.twme.textdisplayshape.util;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MatrixDecompositionTest {

    @Test
    public void testRecomposition() {
        // Create a sheared matrix (like a parallelogram)
        // M = T * R * S * Shear

        Vector3f translation = new Vector3f(10, 20, 30);
        Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(45));
        Vector3f scale = new Vector3f(2, 4, 1);

        Matrix4f original = new Matrix4f()
                .translate(translation)
                .rotate(rotation)
                .scale(scale);

        // Apply shear: x' = x + 0.5y
        // Shear matrix
        Matrix4f shear = new Matrix4f();
        shear.m10(0.5f); // y affects x
        original.mul(shear);

        // Decompose
        TRSResult result = TextDisplayUtil.decompose(original);

        // Reconstruct: T * L * S * R
        Matrix4f reconstructed = new Matrix4f()
                .translate(result.translation())
                .rotate(result.leftRotation())
                .scale(result.scale())
                .rotate(result.rightRotation());

        // Compare matrices
        if (!matricesEqual(original, reconstructed, 0.001f)) {
            System.out.println("Original:\n" + original);
            System.out.println("Reconstructed:\n" + reconstructed);
            fail("Reconstructed matrix does not match original");
        }
        System.out.println("MatrixDecompositionTest: Decomposition verified successfully.");
    }

    private boolean matricesEqual(Matrix4f m1, Matrix4f m2, float epsilon) {
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                // JOML Matrix4f: getRowColumn(row, col)
                if (Math.abs(m1.getRowColumn(r, c) - m2.getRowColumn(r, c)) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }
}
