package dev.twme.textdisplayshape.shape;

import java.util.List;

import org.joml.Vector3f;

/** Connected line segments whose points can change after spawning. */
public interface PolylineShape extends Shape {

    List<Vector3f> getPoints();

    /**
     * Replaces every point. Segments that still exist are animated; segments
     * added by a longer list appear immediately and surplus ones are removed.
     */
    void setPoints(List<Vector3f> points);
}
