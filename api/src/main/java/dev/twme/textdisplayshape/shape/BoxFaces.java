package dev.twme.textdisplayshape.shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.joml.Vector3f;

import dev.twme.textdisplayshape.geometry.BoxGeometry;

/**
 * The six faces of an axis-aligned box, each facing outward.
 * {@link #setBounds} moves every face in place.
 */
public class BoxFaces extends ShapeGroup {

    private final List<ParallelogramShape> faces = new ArrayList<>(6);
    private Vector3f min;
    private Vector3f max;

    /**
     * @param min         one corner of the box
     * @param max         the opposite corner
     * @param faceFactory creates an unspawned parallelogram for each face
     */
    public BoxFaces(Vector3f min, Vector3f max,
                    Function<BoxGeometry.Face, ? extends ParallelogramShape> faceFactory) {
        Objects.requireNonNull(faceFactory, "faceFactory");
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
        for (BoxGeometry.Face face : BoxGeometry.faces(min, max)) {
            ParallelogramShape shape = Objects.requireNonNull(faceFactory.apply(face), "faceFactory result");
            faces.add(shape);
            add(shape);
        }
    }

    public Vector3f getMin() {
        return new Vector3f(min);
    }

    public Vector3f getMax() {
        return new Vector3f(max);
    }

    /** Moves every face to the new box. */
    public void setBounds(Vector3f newMin, Vector3f newMax) {
        this.min = new Vector3f(newMin);
        this.max = new Vector3f(newMax);
        List<BoxGeometry.Face> geometry = BoxGeometry.faces(newMin, newMax);
        for (int index = 0; index < faces.size(); index++) {
            BoxGeometry.Face face = geometry.get(index);
            faces.get(index).setPoints(face.corner(), face.first(), face.second());
        }
    }
}
