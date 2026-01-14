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
}
