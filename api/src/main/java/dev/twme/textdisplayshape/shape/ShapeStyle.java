package dev.twme.textdisplayshape.shape;

/**
 * Reusable appearance and animation settings that can be applied to any
 * {@link ShapeBuilder}, and that composite helpers such as box outlines pass
 * to each of their parts.
 *
 * @param argbColor             background color in ARGB format
 * @param doubleSided           whether a back face is rendered
 * @param blockLight            block light override (0-15)
 * @param skyLight              sky light override (0-15)
 * @param seeThrough            whether the shape renders through blocks
 * @param viewRange             Text Display view range multiplier
 * @param rootAnchor            whether packet shapes ride one root entity
 * @param interpolationDuration ticks used to animate geometry and color updates
 * @param teleportDuration      ticks used to animate {@link Shape#translate}
 */
public record ShapeStyle(
        int argbColor,
        boolean doubleSided,
        int blockLight,
        int skyLight,
        boolean seeThrough,
        float viewRange,
        boolean rootAnchor,
        int interpolationDuration,
        int teleportDuration
) {

    /** The defaults used by every builder. */
    public static final ShapeStyle DEFAULT = new ShapeStyle(
            0xC8FF6464, false, 15, 15, true, 1.0f, false, 0, 0);

    public ShapeStyle {
        if (blockLight < 0 || blockLight > 15 || skyLight < 0 || skyLight > 15) {
            throw new IllegalArgumentException("Light levels must be between 0 and 15");
        }
        if (interpolationDuration < 0) {
            throw new IllegalArgumentException("interpolationDuration must not be negative");
        }
        if (teleportDuration < 0 || teleportDuration > Shape.MAX_TELEPORT_DURATION) {
            throw new IllegalArgumentException(
                    "teleportDuration must be between 0 and " + Shape.MAX_TELEPORT_DURATION);
        }
    }

    public ShapeStyle withColor(int argb) {
        return new ShapeStyle(argb, doubleSided, blockLight, skyLight, seeThrough, viewRange,
                rootAnchor, interpolationDuration, teleportDuration);
    }

    public ShapeStyle withDoubleSided(boolean value) {
        return new ShapeStyle(argbColor, value, blockLight, skyLight, seeThrough, viewRange,
                rootAnchor, interpolationDuration, teleportDuration);
    }

    public ShapeStyle withBrightness(int block, int sky) {
        return new ShapeStyle(argbColor, doubleSided, block, sky, seeThrough, viewRange,
                rootAnchor, interpolationDuration, teleportDuration);
    }

    public ShapeStyle withSeeThrough(boolean value) {
        return new ShapeStyle(argbColor, doubleSided, blockLight, skyLight, value, viewRange,
                rootAnchor, interpolationDuration, teleportDuration);
    }

    public ShapeStyle withViewRange(float value) {
        return new ShapeStyle(argbColor, doubleSided, blockLight, skyLight, seeThrough, value,
                rootAnchor, interpolationDuration, teleportDuration);
    }

    public ShapeStyle withRootAnchor(boolean value) {
        return new ShapeStyle(argbColor, doubleSided, blockLight, skyLight, seeThrough, viewRange,
                value, interpolationDuration, teleportDuration);
    }

    public ShapeStyle withInterpolationDuration(int ticks) {
        return new ShapeStyle(argbColor, doubleSided, blockLight, skyLight, seeThrough, viewRange,
                rootAnchor, ticks, teleportDuration);
    }

    public ShapeStyle withTeleportDuration(int ticks) {
        return new ShapeStyle(argbColor, doubleSided, blockLight, skyLight, seeThrough, viewRange,
                rootAnchor, interpolationDuration, ticks);
    }
}
