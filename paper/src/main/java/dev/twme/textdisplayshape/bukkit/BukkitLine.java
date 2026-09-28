package dev.twme.textdisplayshape.bukkit;

import java.util.List;

import org.bukkit.Location;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.geometry.DisplayTransform;
import dev.twme.textdisplayshape.geometry.ShapeGeometry;
import dev.twme.textdisplayshape.shape.LineShape;

/** Line implementation using server-side Text Display entities. */
public class BukkitLine extends AbstractBukkitShape implements LineShape {
    private final Vector3f p1;
    private final Vector3f p2;
    private final float thickness;
    private final float roll;

    private BukkitLine(Builder builder) {
        super(builder);
        this.p1 = new Vector3f(builder.p1);
        this.p2 = new Vector3f(builder.p2);
        this.thickness = builder.thickness;
        this.roll = builder.roll;
    }

    @Override
    protected List<DisplayTransform> computeTransforms() {
        return ShapeGeometry.line(p1, p2, thickness, roll, isDoubleSided());
    }

    @Override
    protected void offsetGeometry(float dx, float dy, float dz) {
        p1.add(dx, dy, dz);
        p2.add(dx, dy, dz);
    }

    @Override
    public Vector3f getStart() {
        return new Vector3f(p1);
    }

    @Override
    public Vector3f getEnd() {
        return new Vector3f(p2);
    }

    @Override
    public void setPoints(Vector3f start, Vector3f end) {
        List<DisplayTransform> transforms = ShapeGeometry.line(start, end, thickness, roll, isDoubleSided());
        p1.set(start);
        p2.set(end);
        applyTransforms(transforms);
    }

    public static class Builder extends AbstractBukkitShape.Builder<BukkitLine, Builder> {
        private final Vector3f p1;
        private final Vector3f p2;
        private final float thickness;
        private float roll;

        public Builder(Location origin, Vector3f p1, Vector3f p2, float thickness) {
            super(origin);
            this.p1 = p1;
            this.p2 = p2;
            this.thickness = thickness;
        }

        public Builder roll(float roll) {
            this.roll = roll;
            return this;
        }

        public Builder rollDegrees(float degrees) {
            this.roll = (float) Math.toRadians(degrees);
            return this;
        }

        @Override
        public BukkitLine build() {
            return new BukkitLine(this);
        }
    }
}
