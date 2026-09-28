package dev.twme.textdisplayshape.shape;

import org.joml.Vector3f;

/** A line whose endpoints can move after it has spawned. */
public interface LineShape extends Shape {

    Vector3f getStart();

    Vector3f getEnd();

    /**
     * Moves both endpoints. A spawned line updates its existing entities, so
     * the change is animated over {@link #getInterpolationDuration()} ticks.
     */
    void setPoints(Vector3f start, Vector3f end);
}
