package dev.twme.textdisplayshape.shape;

/**
 * Base builder interface for creating shapes.
 * Provides a fluent API to configure shape appearance properties.
 *
 * <p>This interface is platform-agnostic. Colors are represented as ARGB
 * integers instead of platform-specific color classes.</p>
 *
 * @param <T> the type of shape to build
 */
public interface ShapeBuilder<T extends Shape> {

    /**
     * Sets the background color of the shape as an ARGB integer.
     * <p>
     * Format: {@code (alpha << 24) | (red << 16) | (green << 8) | blue}
     *
     * @param argb the color in ARGB format
     * @return this builder
     */
    ShapeBuilder<T> color(int argb);

    /**
     * Sets whether the shape is double-sided.
     *
     * @param doubleSided true for double-sided display
     * @return this builder
     */
    ShapeBuilder<T> doubleSided(boolean doubleSided);

    /**
     * Sets the brightness of the shape.
     *
     * @param block block light level (0-15)
     * @param sky   sky light level (0-15)
     * @return this builder
     */
    ShapeBuilder<T> brightness(int block, int sky);

    /**
     * Sets whether the shape can be seen through blocks.
     *
     * @param seeThrough true if visible through blocks
     * @return this builder
     */
    ShapeBuilder<T> seeThrough(boolean seeThrough);

    /**
     * Sets the view range of the shape display entities.
     *
     * @param viewRange the render view range
     * @return this builder
     */
    ShapeBuilder<T> viewRange(float viewRange);

    /**
     * Enables or disables packet root-anchor mode.
     * <p>
     * Packet builders may use an invisible root entity and attach their display
     * entities as passengers, allowing the whole shape to move by teleporting a
     * single anchor. Implementations that do not support this mode may ignore it.
     *
     * @param enabled true to enable root-anchor mode
     * @return this builder
     */
    default ShapeBuilder<T> rootAnchor(boolean enabled) {
        return this;
    }

    /**
     * Builds the shape instance.
     *
     * @return the built shape
     */
    T build();
}
