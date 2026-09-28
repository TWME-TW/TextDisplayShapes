package dev.twme.textdisplayshape.geometry;

import java.util.Objects;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.util.TRSResult;

/**
 * One Text Display transformation in Minecraft's
 * {@code translation * leftRotation * scale * rightRotation} form.
 *
 * <p>Translations produced by {@link ShapeGeometry} are absolute world
 * coordinates; renderers subtract the entity origin before sending them.
 * Instances are immutable: every accessor returns a defensive copy.</p>
 */
public final class DisplayTransform {

    private final Vector3f translation;
    private final Quaternionf leftRotation;
    private final Vector3f scale;
    private final Quaternionf rightRotation;

    public DisplayTransform(
            Vector3f translation,
            Quaternionf leftRotation,
            Vector3f scale,
            Quaternionf rightRotation
    ) {
        this.translation = new Vector3f(Objects.requireNonNull(translation, "translation"));
        this.leftRotation = new Quaternionf(Objects.requireNonNull(leftRotation, "leftRotation"));
        this.scale = new Vector3f(Objects.requireNonNull(scale, "scale"));
        this.rightRotation = new Quaternionf(Objects.requireNonNull(rightRotation, "rightRotation"));
    }

    /** Converts an analytic TRS decomposition. */
    public static DisplayTransform of(TRSResult result) {
        return new DisplayTransform(
                result.translation(),
                result.leftRotation(),
                result.scale(),
                result.rightRotation()
        );
    }

    /**
     * Converts a matrix without shear, such as a line matrix. The rotation is
     * read without normalizing out scale, matching how lines have always been
     * sent in packet mode.
     */
    public static DisplayTransform ofUnshearedMatrix(Matrix4f matrix) {
        Vector3f translation = matrix.getTranslation(new Vector3f());
        Vector3f scale = matrix.getScale(new Vector3f());
        Quaternionf rotation = matrix.getUnnormalizedRotation(new Quaternionf());
        return new DisplayTransform(translation, rotation, scale, new Quaternionf());
    }

    public Vector3f translation() {
        return new Vector3f(translation);
    }

    public Quaternionf leftRotation() {
        return new Quaternionf(leftRotation);
    }

    public Vector3f scale() {
        return new Vector3f(scale);
    }

    public Quaternionf rightRotation() {
        return new Quaternionf(rightRotation);
    }

    /** Returns a copy whose translation is expressed relative to {@code (x, y, z)}. */
    public DisplayTransform relativeTo(double x, double y, double z) {
        return new DisplayTransform(
                new Vector3f(translation).sub((float) x, (float) y, (float) z),
                leftRotation,
                scale,
                rightRotation
        );
    }

    /** Returns a copy with every scale component multiplied by {@code factor}. */
    public DisplayTransform scaled(float factor) {
        return new DisplayTransform(translation, leftRotation, new Vector3f(scale).mul(factor), rightRotation);
    }

    /** Converts back to a matrix, for platforms that accept one. */
    public Matrix4f toMatrix() {
        return new Matrix4f()
                .translate(translation)
                .rotate(leftRotation)
                .scale(scale)
                .rotate(rightRotation);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DisplayTransform that
                && translation.equals(that.translation)
                && leftRotation.equals(that.leftRotation)
                && scale.equals(that.scale)
                && rightRotation.equals(that.rightRotation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(translation, leftRotation, scale, rightRotation);
    }

    @Override
    public String toString() {
        return "DisplayTransform[translation=" + translation + ", leftRotation=" + leftRotation
                + ", scale=" + scale + ", rightRotation=" + rightRotation + "]";
    }
}
