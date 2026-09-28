package dev.twme.textdisplayshape.shape;

import org.joml.Vector3f;

/** A triangle whose vertices can move after it has spawned. */
public interface TriangleShape extends Shape {

    Vector3f getPoint1();

    Vector3f getPoint2();

    Vector3f getPoint3();

    /** Moves all three vertices, animated when interpolation is enabled. */
    void setPoints(Vector3f point1, Vector3f point2, Vector3f point3);
}
