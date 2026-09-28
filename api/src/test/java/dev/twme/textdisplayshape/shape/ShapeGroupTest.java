package dev.twme.textdisplayshape.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import dev.twme.textdisplayshape.geometry.BoxGeometry;

class ShapeGroupTest {

    /** Minimal in-memory line used to observe group behavior. */
    static final class FakeLine implements LineShape {
        final Set<UUID> viewers = new HashSet<>();
        boolean spawned;
        int color;
        int interpolation;
        int teleport;
        Vector3f start;
        Vector3f end;
        final List<double[]> moves = new ArrayList<>();

        FakeLine(Vector3f start, Vector3f end) {
            this.start = new Vector3f(start);
            this.end = new Vector3f(end);
        }

        @Override public void spawn() { spawned = true; }
        @Override public void remove() { spawned = false; }
        @Override public boolean isSpawned() { return spawned; }
        @Override public void addViewer(UUID id) { viewers.add(id); }
        @Override public void removeViewer(UUID id) { viewers.remove(id); }
        @Override public Set<UUID> getViewerUUIDs() { return new HashSet<>(viewers); }
        @Override public List<UUID> getEntityUUIDs() { return spawned ? List.of(UUID.randomUUID()) : List.of(); }
        @Override public void teleportOrigin(double x, double y, double z) { }
        @Override public void setColor(int argb) { color = argb; }
        @Override public int getColor() { return color; }
        @Override public void setInterpolationDuration(int ticks) { interpolation = ticks; }
        @Override public int getInterpolationDuration() { return interpolation; }
        @Override public void setTeleportDuration(int ticks) { teleport = ticks; }
        @Override public void translate(double dx, double dy, double dz) { moves.add(new double[]{dx, dy, dz}); }
        @Override public Vector3f getStart() { return new Vector3f(start); }
        @Override public Vector3f getEnd() { return new Vector3f(end); }
        @Override public void setPoints(Vector3f s, Vector3f e) { start = new Vector3f(s); end = new Vector3f(e); }
    }

    @Test
    void groupPropagatesLifecycleViewersAndAnimationSettings() {
        FakeLine first = new FakeLine(new Vector3f(), new Vector3f(1, 0, 0));
        ShapeGroup group = new ShapeGroup(List.of(first));
        UUID viewer = UUID.randomUUID();
        group.addViewer(viewer);
        group.spawn();

        FakeLine late = new FakeLine(new Vector3f(), new Vector3f(0, 1, 0));
        group.add(late);
        assertTrue(late.spawned, "members added to a spawned group spawn immediately");
        assertTrue(late.viewers.contains(viewer), "late members inherit the group's viewers");
        assertEquals(2, group.getEntityCount());

        group.setColor(0x80FF0000);
        group.setInterpolationDuration(5);
        group.setTeleportDuration(3);
        group.translate(1, 2, 3);
        for (FakeLine line : List.of(first, late)) {
            assertEquals(0x80FF0000, line.color);
            assertEquals(5, line.interpolation);
            assertEquals(3, line.teleport);
            assertEquals(1, line.moves.size());
        }
        assertEquals(0x80FF0000, group.getColor());

        group.remove();
        assertFalse(first.spawned);
        assertFalse(group.isSpawned());
        assertThrows(IllegalArgumentException.class, () -> group.setTeleportDuration(60));
    }

    @Test
    void boxOutlineMovesEveryEdgeInPlace() {
        List<FakeLine> created = new ArrayList<>();
        BoxOutline outline = new BoxOutline(new Vector3f(0, 0, 0), new Vector3f(1, 1, 1), edge -> {
            FakeLine line = new FakeLine(edge.from(), edge.to());
            created.add(line);
            return line;
        });
        assertEquals(12, created.size());

        outline.setBounds(new Vector3f(2, 2, 2), new Vector3f(4, 4, 4));
        List<BoxGeometry.Edge> expected = BoxGeometry.edges(new Vector3f(2, 2, 2), new Vector3f(4, 4, 4));
        for (int index = 0; index < 12; index++) {
            assertEquals(expected.get(index).from(), created.get(index).start);
            assertEquals(expected.get(index).to(), created.get(index).end);
        }
        assertEquals(new Vector3f(4, 4, 4), outline.getMax());
    }

    @Test
    void styleAppliesEverySetting() {
        List<String> calls = new ArrayList<>();
        ShapeBuilder<FakeLine> builder = new ShapeBuilder<>() {
            @Override public ShapeBuilder<FakeLine> color(int argb) { calls.add("color" + argb); return this; }
            @Override public ShapeBuilder<FakeLine> doubleSided(boolean v) { calls.add("double" + v); return this; }
            @Override public ShapeBuilder<FakeLine> brightness(int b, int s) { calls.add("light" + b + s); return this; }
            @Override public ShapeBuilder<FakeLine> seeThrough(boolean v) { calls.add("see" + v); return this; }
            @Override public ShapeBuilder<FakeLine> viewRange(float v) { calls.add("range" + v); return this; }
            @Override public ShapeBuilder<FakeLine> rootAnchor(boolean v) { calls.add("root" + v); return this; }
            @Override public ShapeBuilder<FakeLine> interpolationDuration(int t) { calls.add("interp" + t); return this; }
            @Override public ShapeBuilder<FakeLine> teleportDuration(int t) { calls.add("tp" + t); return this; }
            @Override public FakeLine build() { return null; }
        };
        builder.style(ShapeStyle.DEFAULT.withColor(7).withInterpolationDuration(4).withTeleportDuration(2)
                .withRootAnchor(true).withBrightness(3, 9));
        assertEquals(List.of("color7", "doublefalse", "light39", "seetrue", "range1.0", "roottrue",
                "interp4", "tp2"), calls);
    }

    @Test
    void styleRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> ShapeStyle.DEFAULT.withBrightness(16, 0));
        assertThrows(IllegalArgumentException.class, () -> ShapeStyle.DEFAULT.withInterpolationDuration(-1));
        assertThrows(IllegalArgumentException.class, () -> ShapeStyle.DEFAULT.withTeleportDuration(60));
    }
}
