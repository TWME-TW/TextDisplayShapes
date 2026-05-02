package dev.twme.textdisplayshape.util;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Data class for storing the calculation results of the textDisplayTriangle
 * method.
 */
public class TextDisplayTriangleResult {
    public final List<Matrix4f> transforms;
    public final Vector3f xAxis;
    public final Vector3f yAxis;
    public final Vector3f zAxis;
    public final float height;
    public final float width;
    public final Quaternionf rotation;
    public final float shear;

    public TextDisplayTriangleResult(
            List<Matrix4f> transforms,
            Vector3f xAxis,
            Vector3f yAxis,
            Vector3f zAxis,
            float height,
            float width,
            Quaternionf rotation,
            float shear) {
        this.transforms = transforms;
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        this.zAxis = zAxis;
        this.height = height;
        this.width = width;
        this.rotation = rotation;
        this.shear = shear;
    }
}
