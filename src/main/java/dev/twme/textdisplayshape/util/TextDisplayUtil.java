package dev.twme.textdisplayshape.util;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Core utility class for TextDisplay shape rendering calculations.
 * Supports triangles, lines, and parallelograms.
 */
public class TextDisplayUtil {

    /**
     * Creates a custom shear transformation matrix and multiplies it with the
     * original matrix.
     */
    private static Matrix4f shear(Matrix4fc mat, float yx, float xy) {
        Matrix4f shearMatrix = new Matrix4f();
        shearMatrix.m10(yx);
        shearMatrix.m01(xy);
        return new Matrix4f(mat).mul(shearMatrix);
    }

    /**
     * Gets the transformation matrix for a unit square.
     *
     * @return the unit square transformation matrix
     */
    public static Matrix4f getTextDisplayUnitSquare() {
        return new Matrix4f().translate(0.4F, 0.0F, 0.0F).scale(8.0F, 4.0F, 1.0F);
    }

    /**
     * Gets the list of transformation matrices for a left-aligned unit triangle.
     *
     * @return list of transformation matrices for the unit triangle
     */
    public static List<Matrix4f> getTextDisplayUnitTriangle() {
        return Stream.of(
                new Matrix4f().scale(0.5f).mul(getTextDisplayUnitSquare()),
                shear(new Matrix4f().scale(0.5f).translate(1f, 0f, 0f), -1f, 0f).mul(getTextDisplayUnitSquare()),
                shear(new Matrix4f().scale(0.5f).translate(0f, 1f, 0f), 0f, -1f).mul(getTextDisplayUnitSquare()))
                .collect(Collectors.toList());
    }

    /**
     * Calculates the transformation matrices for a triangle.
     *
     * @param point1 the first vertex
     * @param point2 the second vertex
     * @param point3 the third vertex
     * @return result object containing transformation matrices and related
     *         information
     */
    public static TextDisplayTriangleResult textDisplayTriangle(
            Vector3f point1,
            Vector3f point2,
            Vector3f point3) {
        Vector3f p2 = new Vector3f(point2).sub(point1);
        Vector3f p3 = new Vector3f(point3).sub(point1);

        if (new Vector3f(p2).cross(p3).lengthSquared() < 1.0E-4F) {
            p3.add(0.0001f, 0.0001f, 0.0001f);
        }

        Vector3f zAxis = new Vector3f(p2).cross(p3).normalize();
        Vector3f xAxis = new Vector3f(p2).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        float width = p2.length();
        float height = p3.dot(yAxis);
        float p3Width = p3.dot(xAxis);

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();

        float shear = (width > 0.001f) ? p3Width / width : 0.0f;

        Matrix4f transform = new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(width, height, 1f);
        transform = shear(transform, shear, 0.0F);

        Matrix4f finalTransform = transform;
        List<Matrix4f> transforms = getTextDisplayUnitTriangle().stream()
                .map(unit -> new Matrix4f(finalTransform).mul(unit))
                .collect(Collectors.toList());

        return new TextDisplayTriangleResult(
                transforms, xAxis, yAxis, zAxis, height, width, rotation, shear);
    }

    /**
     * Calculates the transformation matrix for a line.
     * A line is a very thin rectangle, automatically centered based on thickness.
     *
     * @param point1    the start point of the line
     * @param point2    the end point of the line
     * @param thickness the thickness of the line
     * @return the transformation matrix
     */
    public static Matrix4f textDisplayLine(Vector3f point1, Vector3f point2, float thickness) {
        return textDisplayLine(point1, point2, thickness, 0f);
    }

    /**
     * Calculates the transformation matrix for a line with roll angle.
     * A line is a very thin rectangle, automatically centered based on thickness.
     * The roll angle rotates the line surface around its length axis.
     *
     * @param point1    the start point of the line
     * @param point2    the end point of the line
     * @param thickness the thickness of the line
     * @param roll      the roll angle in radians (rotation around the line axis)
     * @return the transformation matrix
     */
    public static Matrix4f textDisplayLine(Vector3f point1, Vector3f point2, float thickness, float roll) {
        Vector3f direction = new Vector3f(point2).sub(point1);
        float length = direction.length();

        if (length < 0.001f) {
            return new Matrix4f();
        }

        // Find an axis perpendicular to the line direction as the "up" direction
        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(direction.dot(up) / length) > 0.99f) {
            up = new Vector3f(1, 0, 0);
        }

        Vector3f zAxis = new Vector3f(direction).cross(up).normalize();
        Vector3f xAxis = new Vector3f(direction).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();

        // Apply roll rotation around the line axis (X axis in local space)
        Quaternionf rollRotation = new Quaternionf().rotateX(roll);
        rotation = rotation.mul(rollRotation);

        // Line transformation: translate to start point, rotate to correct direction,
        // center thickness, scale to correct length and thickness
        // translate(0, -0.5, 0) centers the line in the Y direction (thickness
        // direction)
        return new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(length, thickness, 1f)
                .translate(0f, -0.5f, 0f) // Center: offset down by half unit (since unit square starts at 0)
                .mul(getTextDisplayUnitSquare());
    }

    /**
     * Calculates the transformation matrix for a parallelogram.
     * A parallelogram is defined by three points: p1 is the starting corner,
     * p2 and p3 define the two edges.
     * The fourth point is automatically calculated as p1 + (p2-p1) + (p3-p1).
     *
     * @param point1 the starting point (one corner)
     * @param point2 the second point (defines the first edge, width direction)
     * @param point3 the third point (defines the second edge, height direction)
     * @return the transformation matrix
     */
    public static Matrix4f textDisplayParallelogram(Vector3f point1, Vector3f point2, Vector3f point3) {
        Vector3f p2 = new Vector3f(point2).sub(point1); // Width vector
        Vector3f p3 = new Vector3f(point3).sub(point1); // Height vector

        // Handle collinear case
        if (new Vector3f(p2).cross(p3).lengthSquared() < 1.0E-4F) {
            p3.add(0.0001f, 0.0001f, 0.0001f);
        }

        Vector3f zAxis = new Vector3f(p2).cross(p3).normalize();
        Vector3f xAxis = new Vector3f(p2).normalize();
        Vector3f yAxis = new Vector3f(zAxis).cross(xAxis).normalize();

        float width = p2.length();
        float height = p3.dot(yAxis);
        float p3Width = p3.dot(xAxis);

        Quaternionf rotation = new Quaternionf().lookAlong(new Vector3f(zAxis).mul(-1f), yAxis).conjugate();

        // Calculate shear amount (transforms rectangle into parallelogram)
        float shear = (width > 0.001f) ? p3Width / width : 0.0f;

        // Parallelogram uses the complete unit square (unlike triangle which requires
        // assembly)
        Matrix4f transform = new Matrix4f()
                .translate(point1)
                .rotate(rotation)
                .scale(width, height, 1f);
        transform = shear(transform, shear, 0.0F);

        return transform.mul(getTextDisplayUnitSquare());
    }

    /**
     * Decomposes a Matrix4f into the format used by Minecraft's Display Entity:
     * leftRotation * scale * rightRotation + translation.
     * This uses polar decomposition to properly handle matrices with shear.
     *
     * @param matrix the transformation matrix to decompose
     * @return a TRSResult containing translation, scale, leftRotation, and
     *         rightRotation
     */
    public static TRSResult decompose(Matrix4f matrix) {
        // Extract translation from the 4th column
        Vector3f translation = new Vector3f();
        matrix.getTranslation(translation);

        // Extract the 3x3 upper-left portion (rotation + scale + shear)
        org.joml.Matrix3f upper3x3 = new org.joml.Matrix3f();
        matrix.get3x3(upper3x3);

        // Perform polar decomposition: M = Q * S where Q is orthogonal (rotation) and S
        // is symmetric
        // We use an iterative method to find Q
        org.joml.Matrix3f Q = new org.joml.Matrix3f(upper3x3);
        // Iterations count: 10 is usually sufficient for high precision
        for (int i = 0; i < 10; i++) {
            // Q_next = 0.5 * (Q + Q^-T)
            org.joml.Matrix3f Qinv = new org.joml.Matrix3f(Q).invert();
            if (Float.isNaN(Qinv.m00))
                break; // Handle singular matrix
            org.joml.Matrix3f QinvT = new org.joml.Matrix3f(Qinv).transpose();
            Q.add(QinvT).scale(0.5f);
        }

        // S = Q^T * M (the symmetric part containing scale and shear)
        org.joml.Matrix3f S = new org.joml.Matrix3f(Q).transpose().mul(upper3x3);

        // We need to diagonalize S to find scale (eigenvalues) and rightRotation
        // (eigenvectors).
        // Since S is symmetric, S = V * D * V^T, where V is orthogonal.
        // The decomposition we want is M = L * Sc * R
        // M = Q * S = Q * (V * D * V^T) = (Q * V) * D * V^T
        // So:
        // Scale = D (diagonal elements)
        // LeftRotation = Q * V
        // RightRotation = V^T

        // Diagonalize S using Jacobi method (simplified 3x3 implementation or use
        // library if available)

        // Initialize V as identity
        org.joml.Matrix3f V = new org.joml.Matrix3f(); // Eigenvectors
        V.identity();

        // Jacobi Iteration
        org.joml.Matrix3f A = new org.joml.Matrix3f(S);
        int maxIter = 20;
        for (int iter = 0; iter < maxIter; iter++) {
            // Find pivot (largest off-diagonal element)
            float max = 0.0f;
            int p = 0, q = 1;
            for (int i = 0; i < 3; i++) {
                for (int j = i + 1; j < 3; j++) {
                    float val = Math.abs(A.getRowColumn(i, j));
                    if (val > max) {
                        max = val;
                        p = i;
                        q = j;
                    }
                }
            }

            if (max < 1e-6f)
                break; // Converged

            // Compute rotation
            float app = A.getRowColumn(p, p);
            float aqq = A.getRowColumn(q, q);
            float apq = A.getRowColumn(p, q);
            float phi = 0.5f * (float) Math.atan2(2.0f * apq, aqq - app);
            float c = (float) Math.cos(phi);
            float s = (float) Math.sin(phi);

            // Rotation matrix J
            org.joml.Matrix3f J = new org.joml.Matrix3f();
            J.setRowColumn(p, p, c);
            J.setRowColumn(q, q, c);
            J.setRowColumn(p, q, s);
            J.setRowColumn(q, p, -s);

            org.joml.Matrix3f JT = new org.joml.Matrix3f(J).transpose();
            A = JT.mul(A).mul(J);
            V.mul(J);
        }

        // Now D is diagonal of A
        Vector3f D = new Vector3f(A.m00, A.m11, A.m22);

        // Fix negative scales (flip scale and eigenvector)
        if (D.x < 0) {
            D.x = -D.x;
            negateColumn(V, 0);
        }
        if (D.y < 0) {
            D.y = -D.y;
            negateColumn(V, 1);
        }
        if (D.z < 0) {
            D.z = -D.z;
            negateColumn(V, 2);
        }

        // Calculate components
        Vector3f scale = new Vector3f(D);

        // Left Rotation = Q * V
        org.joml.Matrix3f leftRotMat = new org.joml.Matrix3f(Q).mul(V);
        Quaternionf leftRotation = new Quaternionf().setFromNormalized(leftRotMat);
        leftRotation.normalize();

        // Right Rotation = V^T
        org.joml.Matrix3f rightRotMat = new org.joml.Matrix3f(V).transpose();
        Quaternionf rightRotation = new Quaternionf().setFromNormalized(rightRotMat);
        rightRotation.normalize();

        return new TRSResult(translation, leftRotation, scale, rightRotation);
    }

    private static void negateColumn(org.joml.Matrix3f m, int col) {
        float x = m.getRowColumn(0, col);
        float y = m.getRowColumn(1, col);
        float z = m.getRowColumn(2, col);
        m.setColumn(col, -x, -y, -z);
    }
}
