package com.bashvollink.curtaindoors.client.sim;

import java.util.List;

/**
 * Position-based Verlet cloth solver. Pure Java, no Minecraft classes, so it can be unit tested.
 * All coordinates are in curtain-local space (see {@link ClothLayout}).
 */
public final class ClothSim {
    private static final float EPS = 1.0e-8F;

    public final ClothLayout layout;
    public final SimParams params;
    public final int count;
    /** Current positions, xyz interleaved. */
    public final float[] pos;
    /** Positions at the previous substep (Verlet velocity). */
    private final float[] prev;
    /** Positions at the start of the current game tick, for render interpolation. */
    private final float[] last;
    private final float[] invMass;
    private final float floorY;

    public ClothSim(ClothLayout layout, SimParams params) {
        this.layout = layout;
        this.params = params;
        this.count = layout.particleCount;
        this.pos = layout.rest.clone();
        this.prev = layout.rest.clone();
        this.last = layout.rest.clone();
        this.invMass = new float[count];
        for (int i = 0; i < count; i++) {
            invMass[i] = layout.pinned[i] ? 0F : 1F;
        }
        this.floorY = ClothLayout.BOTTOM_GAP * 0.5F;
    }

    /** Snap back to the rest pose with no motion. */
    public void reset() {
        System.arraycopy(layout.rest, 0, pos, 0, pos.length);
        System.arraycopy(layout.rest, 0, prev, 0, prev.length);
        System.arraycopy(layout.rest, 0, last, 0, last.length);
    }

    /** Call once at the start of every game tick, before the substeps. */
    public void beginTick() {
        System.arraycopy(pos, 0, last, 0, pos.length);
    }

    /**
     * Advance one substep.
     *
     * @param dt        substep length in seconds
     * @param colliders entities touching the curtain, in local space
     * @param wind      wind acceleration along the curtain normal (blocks/s^2); 0 for none
     * @param time      seconds, used to vary wind across the cloth
     */
    public void step(float dt, List<Cylinder> colliders, float wind, float time) {
        float dt2 = dt * dt;
        float damping = params.damping();
        float gy = -params.gravity() * dt2;
        float windScale = wind * params.windResponse() * dt2;

        for (int i = 0; i < count; i++) {
            if (invMass[i] == 0F) {
                continue;
            }
            int k = i * 3;
            float x = pos[k], y = pos[k + 1], z = pos[k + 2];
            float vx = (x - prev[k]) * damping;
            float vy = (y - prev[k + 1]) * damping;
            float vz = (z - prev[k + 2]) * damping;
            prev[k] = x;
            prev[k + 1] = y;
            prev[k + 2] = z;
            float ax = 0F, az = 0F;
            if (windScale != 0F) {
                float gust = 0.6F + 0.4F * (float) Math.sin(time * 1.7F + x * 3.1F + y * 1.3F);
                az = windScale * gust;
                ax = windScale * 0.25F * (float) Math.sin(time * 0.9F + y * 2.3F);
            }
            pos[k] = x + vx + ax;
            pos[k + 1] = y + vy + gy;
            pos[k + 2] = z + vz + az;
        }

        int iterations = params.iterations();
        for (int it = 0; it < iterations; it++) {
            solveLinks();
            solveTethers();
            if (!colliders.isEmpty()) {
                collide(colliders, dt, it == iterations - 1);
            }
        }
        clampFloor();
    }

    private void solveLinks() {
        int[] la = layout.linkA, lb = layout.linkB;
        float[] lr = layout.linkRest, ls = layout.linkStiffness;
        for (int c = 0; c < la.length; c++) {
            int a = la[c], b = lb[c];
            float wa = invMass[a], wb = invMass[b];
            float w = wa + wb;
            if (w == 0F) {
                continue;
            }
            int ka = a * 3, kb = b * 3;
            float dx = pos[kb] - pos[ka];
            float dy = pos[kb + 1] - pos[ka + 1];
            float dz = pos[kb + 2] - pos[ka + 2];
            float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d < EPS) {
                continue;
            }
            float corr = (d - lr[c]) / d * ls[c] / w;
            pos[ka] += dx * corr * wa;
            pos[ka + 1] += dy * corr * wa;
            pos[ka + 2] += dz * corr * wa;
            pos[kb] -= dx * corr * wb;
            pos[kb + 1] -= dy * corr * wb;
            pos[kb + 2] -= dz * corr * wb;
        }
    }

    private void solveTethers() {
        int[] anchor = layout.tetherAnchor;
        float[] maxLen = layout.tetherLength;
        for (int i = 0; i < count; i++) {
            int a = anchor[i];
            if (a < 0 || invMass[i] == 0F) {
                continue;
            }
            int k = i * 3, ka = a * 3;
            float dx = pos[k] - pos[ka], dy = pos[k + 1] - pos[ka + 1], dz = pos[k + 2] - pos[ka + 2];
            float d2 = dx * dx + dy * dy + dz * dz;
            float max = maxLen[i];
            if (d2 > max * max) {
                float scale = max / (float) Math.sqrt(d2);
                pos[k] = pos[ka] + dx * scale;
                pos[k + 1] = pos[ka + 1] + dy * scale;
                pos[k + 2] = pos[ka + 2] + dz * scale;
            }
        }
    }

    private void collide(List<Cylinder> colliders, float dt, boolean applyDrag) {
        float drag = params.drag() * dt;
        for (Cylinder cyl : colliders) {
            float r = cyl.radius + params.colliderMargin();
            float r2 = r * r;
            for (int i = 0; i < count; i++) {
                if (invMass[i] == 0F) {
                    continue;
                }
                int k = i * 3;
                float y = pos[k + 1];
                if (y < cyl.yMin || y > cyl.yMax) {
                    continue;
                }
                float dx = pos[k] - cyl.cx;
                float dz = pos[k + 2] - cyl.cz;
                float d2 = dx * dx + dz * dz;
                if (d2 >= r2) {
                    continue;
                }
                float nx, nz;
                if (d2 < EPS) {
                    // Dead centre: push the cloth the way the entity is walking.
                    nx = 0F;
                    nz = cyl.vz >= 0F ? 1F : -1F;
                } else {
                    float d = (float) Math.sqrt(d2);
                    nx = dx / d;
                    nz = dz / d;
                }
                pos[k] = cyl.cx + nx * r;
                pos[k + 2] = cyl.cz + nz * r;
                if (applyDrag) {
                    // Hand some of the entity's motion to the cloth so it trails behind the body.
                    prev[k] -= cyl.vx * drag;
                    prev[k + 2] -= cyl.vz * drag;
                }
            }
        }
    }

    private void clampFloor() {
        for (int i = 0; i < count; i++) {
            int k = i * 3;
            if (pos[k + 1] < floorY) {
                pos[k + 1] = floorY;
                // Ground friction.
                prev[k] += (pos[k] - prev[k]) * 0.5F;
                prev[k + 2] += (pos[k + 2] - prev[k + 2]) * 0.5F;
            }
        }
    }

    /** Largest squared distance any particle moved since {@link #beginTick()}. */
    public float maxTickDisplacementSq() {
        float max = 0F;
        for (int k = 0; k < pos.length; k += 3) {
            float dx = pos[k] - last[k], dy = pos[k + 1] - last[k + 1], dz = pos[k + 2] - last[k + 2];
            max = Math.max(max, dx * dx + dy * dy + dz * dz);
        }
        return max;
    }

    /** Positions blended between the start and end of the current tick. */
    public void interpolate(float t, float[] out) {
        for (int k = 0; k < pos.length; k++) {
            out[k] = last[k] + (pos[k] - last[k]) * t;
        }
    }

    public boolean isPinned(int i) {
        return invMass[i] == 0F;
    }

    /** True if any coordinate is NaN or infinite. */
    public boolean hasInvalidState() {
        for (float v : pos) {
            if (!Float.isFinite(v)) {
                return true;
            }
        }
        return false;
    }
}
