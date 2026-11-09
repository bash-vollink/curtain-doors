package com.bashvollink.curtaindoors.client.sim;

/**
 * A vertical cylinder in curtain-local space, standing in for an entity passing through.
 * Velocity is in blocks per second, also in local space.
 */
public final class Cylinder {
    public float cx, cz, radius, yMin, yMax, vx, vz;

    public Cylinder set(float cx, float cz, float radius, float yMin, float yMax, float vx, float vz) {
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
        this.yMin = yMin;
        this.yMax = yMax;
        this.vx = vx;
        this.vz = vz;
        return this;
    }
}
