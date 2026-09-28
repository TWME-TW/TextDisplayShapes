package dev.twme.textdisplayshape.shape;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Several shapes managed as one: lifecycle, viewers, color, animation
 * settings, and movement are applied to every member.
 *
 * <p>Viewers added to the group are added to every member, including members
 * added later. The group does not batch packets itself; packet renderers
 * that share a {@code VirtualEntityManager} may wrap calls in
 * {@code VirtualEntityManager#bundle} for atomic updates.</p>
 */
public class ShapeGroup implements Shape {

    private final List<Shape> members = new ArrayList<>();
    private final Set<UUID> viewers = new LinkedHashSet<>();
    private boolean spawned;
    private int interpolationDuration;
    private int teleportDuration;

    public ShapeGroup() {
    }

    public ShapeGroup(List<? extends Shape> shapes) {
        for (Shape shape : shapes) {
            add(shape);
        }
    }

    /** Adds a member, applying the group's viewers and spawning it when the group is spawned. */
    public ShapeGroup add(Shape shape) {
        Objects.requireNonNull(shape, "shape");
        members.add(shape);
        for (UUID viewer : viewers) {
            shape.addViewer(viewer);
        }
        if (spawned && !shape.isSpawned()) {
            shape.spawn();
        }
        return this;
    }

    /** Removes a member from the group and from the world. */
    public boolean remove(Shape shape) {
        if (!members.remove(shape)) {
            return false;
        }
        shape.remove();
        return true;
    }

    /** Returns a copy of the members in insertion order. */
    public List<Shape> getMembers() {
        return new ArrayList<>(members);
    }

    @Override
    public void spawn() {
        if (spawned) {
            return;
        }
        for (Shape shape : members) {
            shape.spawn();
        }
        spawned = true;
    }

    @Override
    public void remove() {
        for (Shape shape : members) {
            shape.remove();
        }
        spawned = false;
    }

    @Override
    public boolean isSpawned() {
        return spawned;
    }

    @Override
    public void addViewer(UUID playerUUID) {
        viewers.add(playerUUID);
        for (Shape shape : members) {
            shape.addViewer(playerUUID);
        }
    }

    @Override
    public void removeViewer(UUID playerUUID) {
        viewers.remove(playerUUID);
        for (Shape shape : members) {
            shape.removeViewer(playerUUID);
        }
    }

    @Override
    public Set<UUID> getViewerUUIDs() {
        return new LinkedHashSet<>(viewers);
    }

    @Override
    public List<UUID> getEntityUUIDs() {
        List<UUID> uuids = new ArrayList<>();
        for (Shape shape : members) {
            uuids.addAll(shape.getEntityUUIDs());
        }
        return uuids;
    }

    @Override
    public int getEntityCount() {
        int count = 0;
        for (Shape shape : members) {
            count += shape.getEntityCount();
        }
        return count;
    }

    @Override
    public void teleportOrigin(double x, double y, double z) {
        for (Shape shape : members) {
            shape.teleportOrigin(x, y, z);
        }
    }

    @Override
    public void setColor(int argb) {
        for (Shape shape : members) {
            shape.setColor(argb);
        }
    }

    /** Returns the color of the first member. */
    @Override
    public int getColor() {
        if (members.isEmpty()) {
            throw new IllegalStateException("Empty shape group has no color");
        }
        return members.get(0).getColor();
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("ticks must not be negative");
        }
        interpolationDuration = ticks;
        for (Shape shape : members) {
            shape.setInterpolationDuration(ticks);
        }
    }

    @Override
    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    @Override
    public void setTeleportDuration(int ticks) {
        if (ticks < 0 || ticks > MAX_TELEPORT_DURATION) {
            throw new IllegalArgumentException("ticks must be between 0 and " + MAX_TELEPORT_DURATION);
        }
        teleportDuration = ticks;
        for (Shape shape : members) {
            shape.setTeleportDuration(ticks);
        }
    }

    @Override
    public int getTeleportDuration() {
        return teleportDuration;
    }

    @Override
    public void translate(double dx, double dy, double dz) {
        for (Shape shape : members) {
            shape.translate(dx, dy, dz);
        }
    }
}
