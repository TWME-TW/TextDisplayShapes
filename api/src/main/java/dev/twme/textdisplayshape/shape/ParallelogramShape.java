package dev.twme.textdisplayshape.shape;

import org.joml.Vector3f;

/**
 * A parallelogram whose defining corner and edges can move after it has
 * spawned. The fourth corner is {@code point2 + point3 - point1}.
 */
public interface ParallelogramShape extends Shape {

    Vector3f getPoint1();

    Vector3f getPoint2();

    Vector3f getPoint3();

    /** Moves the defining points, animated when interpolation is enabled. */
    void setPoints(Vector3f point1, Vector3f point2, Vector3f point3);
}
