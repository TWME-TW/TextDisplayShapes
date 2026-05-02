package dev.twme.textdisplayshape.util;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Result of decomposing a transformation matrix into Minecraft's TextDisplay format.
 * Format: Translation * LeftRotation * Scale * RightRotation
 */
public record TRSResult(
    Vector3f translation,
    Quaternionf leftRotation,
    Vector3f scale,
    Quaternionf rightRotation
) {}
