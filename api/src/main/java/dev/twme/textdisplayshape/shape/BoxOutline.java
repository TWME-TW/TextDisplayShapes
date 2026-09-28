package dev.twme.textdisplayshape.shape;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.joml.Vector3f;

import dev.twme.textdisplayshape.geometry.BoxGeometry;

/**
 * The twelve edges of an axis-aligned box. {@link #setBounds} moves every
 * edge in place, so a spawned outline animates when interpolation is set.
 */
public class BoxOutline extends ShapeGroup {

    private final List<LineShape> edges = new ArrayList<>(12);
    private Vector3f min;
    private Vector3f max;

    /**
     * @param min         one corner of the box
     * @param max         the opposite corner
     * @param lineFactory creates an unspawned line for each edge
     */
    public BoxOutline(Vector3f min, Vector3f max, Function<BoxGeometry.Edge, ? extends LineShape> lineFactory) {
        Objects.requireNonNull(lineFactory, "lineFactory");
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
        for (BoxGeometry.Edge edge : BoxGeometry.edges(min, max)) {
            LineShape line = Objects.requireNonNull(lineFactory.apply(edge), "lineFactory result");
            edges.add(line);
            add(line);
        }
    }

    public Vector3f getMin() {
        return new Vector3f(min);
    }

    public Vector3f getMax() {
        return new Vector3f(max);
    }

    /** Moves every edge to the new box. */
    public void setBounds(Vector3f newMin, Vector3f newMax) {
        this.min = new Vector3f(newMin);
        this.max = new Vector3f(newMax);
        List<BoxGeometry.Edge> geometry = BoxGeometry.edges(newMin, newMax);
        for (int index = 0; index < edges.size(); index++) {
            edges.get(index).setPoints(geometry.get(index).from(), geometry.get(index).to());
        }
    }
}
