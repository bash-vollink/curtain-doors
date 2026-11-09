package com.bashvollink.curtaindoors.client.sim;

import net.minecraft.core.Direction;

/**
 * Maps between curtain-local coordinates (x across, y up from the bottom block, z normal) and
 * coordinates relative to the curtain's bottom block corner.
 */
public final class CurtainSpace {
    private CurtainSpace() {
    }

    /** World-relative X for a local point. */
    public static float worldX(Direction.Axis axis, float x, float z) {
        return axis == Direction.Axis.X ? x : 0.5F + z;
    }

    /** World-relative Z for a local point. */
    public static float worldZ(Direction.Axis axis, float x, float z) {
        return axis == Direction.Axis.X ? 0.5F + z : x;
    }

    /** Local X (across the curtain) for a world-relative point. */
    public static float localX(Direction.Axis axis, double wx, double wz) {
        return (float) (axis == Direction.Axis.X ? wx : wz);
    }

    /** Local Z (curtain normal) for a world-relative point. */
    public static float localZ(Direction.Axis axis, double wx, double wz) {
        return (float) (axis == Direction.Axis.X ? wz - 0.5 : wx - 0.5);
    }

    /** Local X for a world-space direction (no offset). */
    public static float dirX(Direction.Axis axis, double dx, double dz) {
        return (float) (axis == Direction.Axis.X ? dx : dz);
    }

    /** Local Z for a world-space direction (no offset). */
    public static float dirZ(Direction.Axis axis, double dx, double dz) {
        return (float) (axis == Direction.Axis.X ? dz : dx);
    }
}
