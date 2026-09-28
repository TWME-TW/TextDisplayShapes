package dev.twme.textdisplayshape.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.joml.Vector3f;

import dev.twme.textdisplayshape.geometry.DisplayTransform;
import dev.twme.textdisplayshape.geometry.ShapeGeometry;
import dev.twme.textdisplayshape.shape.PolylineShape;

/** Polyline (connected line segments) using server-side Text Display entities (Spigot-compatible, no Adventure API). */
public class BukkitPolyline extends AbstractBukkitShape implements PolylineShape {
    private final List<Vector3f> points = new ArrayList<>();
    private final float thickness;
    private final float roll;
    private final boolean closed;

    private BukkitPolyline(Builder builder) {
        super(builder);
        for (Vector3f point : builder.points) {
            points.add(new Vector3f(point));
        }
        this.thickness = builder.thickness;
        this.roll = builder.roll;
        this.closed = builder.closed;
    }

    @Override
    protected List<DisplayTransform> computeTransforms() {
        return ShapeGeometry.polyline(points, thickness, roll, closed, isDoubleSided());
    }

    @Override
    protected void offsetGeometry(float dx, float dy, float dz) {
        for (Vector3f point : points) {
            point.add(dx, dy, dz);
        }
    }

    @Override
    public List<Vector3f> getPoints() {
        List<Vector3f> copy = new ArrayList<>(points.size());
        for (Vector3f point : points) {
            copy.add(new Vector3f(point));
        }
        return copy;
    }

    @Override
    public void setPoints(List<Vector3f> newPoints) {
        List<DisplayTransform> transforms =
                ShapeGeometry.polyline(newPoints, thickness, roll, closed, isDoubleSided());
        points.clear();
        for (Vector3f point : newPoints) {
            points.add(new Vector3f(point));
        }
        applyTransforms(transforms);
    }

    public int getSegmentCount() {
        if (points.size() < 2) {
            return 0;
        }
        return closed && points.size() > 2 ? points.size() : points.size() - 1;
    }

    public static class Builder extends AbstractBukkitShape.Builder<BukkitPolyline, Builder> {
        private final List<Vector3f> points;
        private final float thickness;
        private float roll;
        private boolean closed;

        public Builder(Location origin, List<Vector3f> points, float thickness) {
            super(origin);
            this.points = new ArrayList<>(points);
            this.thickness = thickness;
        }

        public Builder closed(boolean value) {
            this.closed = value;
            return this;
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
        public BukkitPolyline build() {
            return new BukkitPolyline(this);
        }
    }
}
