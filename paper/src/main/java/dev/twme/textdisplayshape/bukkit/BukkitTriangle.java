package dev.twme.textdisplayshape.bukkit;

import java.util.List;

import org.bukkit.Location;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.geometry.DisplayTransform;
import dev.twme.textdisplayshape.geometry.ShapeGeometry;
import dev.twme.textdisplayshape.shape.TriangleShape;

/** Triangle implementation using server-side Text Display entities. */
public class BukkitTriangle extends AbstractBukkitShape implements TriangleShape {
    private final Vector3f p1;
    private final Vector3f p2;
    private final Vector3f p3;

    private BukkitTriangle(Builder builder) {
        super(builder);
        this.p1 = new Vector3f(builder.p1);
        this.p2 = new Vector3f(builder.p2);
        this.p3 = new Vector3f(builder.p3);
    }

    @Override
    protected List<DisplayTransform> computeTransforms() {
        return ShapeGeometry.triangle(p1, p2, p3, isDoubleSided());
    }

    @Override
    protected void offsetGeometry(float dx, float dy, float dz) {
        p1.add(dx, dy, dz);
        p2.add(dx, dy, dz);
        p3.add(dx, dy, dz);
    }

    @Override
    public Vector3f getPoint1() {
        return new Vector3f(p1);
    }

    @Override
    public Vector3f getPoint2() {
        return new Vector3f(p2);
    }

    @Override
    public Vector3f getPoint3() {
        return new Vector3f(p3);
    }

    @Override
    public void setPoints(Vector3f point1, Vector3f point2, Vector3f point3) {
        List<DisplayTransform> transforms = ShapeGeometry.triangle(point1, point2, point3, isDoubleSided());
        p1.set(point1);
        p2.set(point2);
        p3.set(point3);
        applyTransforms(transforms);
    }

    public static class Builder extends AbstractBukkitShape.Builder<BukkitTriangle, Builder> {
        private final Vector3f p1;
        private final Vector3f p2;
        private final Vector3f p3;

        public Builder(Location origin, Vector3f p1, Vector3f p2, Vector3f p3) {
            super(origin);
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        @Override
        public BukkitTriangle build() {
            return new BukkitTriangle(this);
        }
    }
}
