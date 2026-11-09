package com.bashvollink.curtaindoors.client.render;

import com.bashvollink.curtaindoors.client.sim.ClothLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Turns cloth panel groups into quads. Every cell is drawn twice, once per side with opposite
 * winding and normal, so a culling render type shows each side lit correctly with no z-fighting.
 */
final class SheetMesher {
    private SheetMesher() {
    }

    /**
     * @param p          particle positions relative to the block entity origin, xyz interleaved
     * @param lights     packed light per curtain block, index 0 = bottom block
     * @param yToLocal   add to a position's y to get its curtain-local y
     * @param uPerGroup  true: texture spans each group (plastic strips); false: spans the whole curtain
     */
    static void render(VertexConsumer vc, PoseStack.Pose pose, ClothLayout layout, float[] p, int[] lights,
                       float yToLocal, TextureAtlasSprite sprite, int argb, boolean uPerGroup,
                       float fallbackNx, float fallbackNz) {
        float[] n = new float[p.length];
        for (ClothLayout.Group g : layout.groups) {
            computeNormals(g, p, n, fallbackNx, fallbackNz);
        }
        for (ClothLayout.Group g : layout.groups) {
            int rowsPerBlock = Math.max(1, (g.rows() - 1) / layout.length);
            for (int r = 0; r + 1 < g.rows(); r++) {
                int block = r / rowsPerBlock;
                float vTop = sprite.getV((r - block * rowsPerBlock) / (float) rowsPerBlock);
                float vBottom = sprite.getV((r + 1 - block * rowsPerBlock) / (float) rowsPerBlock);
                for (int c = 0; c + 1 < g.cols(); c++) {
                    int a = g.index(r + 1, c);     // bottom-left
                    int b = g.index(r + 1, c + 1); // bottom-right
                    int cc = g.index(r, c + 1);    // top-right
                    int d = g.index(r, c);         // top-left
                    float uLeft, uRight;
                    if (uPerGroup) {
                        uLeft = sprite.getU(c / (float) (g.cols() - 1));
                        uRight = sprite.getU((c + 1) / (float) (g.cols() - 1));
                    } else {
                        uLeft = sprite.getU(clamp01(layout.rest[d * 3]));
                        uRight = sprite.getU(clamp01(layout.rest[cc * 3]));
                    }
                    // Front.
                    emit(vc, pose, p, n, a, uLeft, vBottom, argb, lights, yToLocal, 1F);
                    emit(vc, pose, p, n, b, uRight, vBottom, argb, lights, yToLocal, 1F);
                    emit(vc, pose, p, n, cc, uRight, vTop, argb, lights, yToLocal, 1F);
                    emit(vc, pose, p, n, d, uLeft, vTop, argb, lights, yToLocal, 1F);
                    // Back.
                    emit(vc, pose, p, n, d, uLeft, vTop, argb, lights, yToLocal, -1F);
                    emit(vc, pose, p, n, cc, uRight, vTop, argb, lights, yToLocal, -1F);
                    emit(vc, pose, p, n, b, uRight, vBottom, argb, lights, yToLocal, -1F);
                    emit(vc, pose, p, n, a, uLeft, vBottom, argb, lights, yToLocal, -1F);
                }
            }
        }
    }

    private static float clamp01(float v) {
        return Math.max(0F, Math.min(1F, v));
    }

    private static void emit(VertexConsumer vc, PoseStack.Pose pose, float[] p, float[] n, int i, float u, float v,
                             int argb, int[] lights, float yToLocal, float side) {
        int k = i * 3;
        int block = (int) Math.floor(p[k + 1] + yToLocal);
        int light = lights[Math.max(0, Math.min(lights.length - 1, block))];
        MeshUtil.vertex(vc, pose, p[k], p[k + 1], p[k + 2], argb, u, v, light,
                n[k] * side, n[k + 1] * side, n[k + 2] * side);
    }

    /** Smooth per-vertex normals from neighbouring particles: across x up. */
    private static void computeNormals(ClothLayout.Group g, float[] p, float[] n, float fallbackNx, float fallbackNz) {
        for (int r = 0; r < g.rows(); r++) {
            for (int c = 0; c < g.cols(); c++) {
                int right = g.index(r, Math.min(c + 1, g.cols() - 1)) * 3;
                int left = g.index(r, Math.max(c - 1, 0)) * 3;
                int up = g.index(Math.max(r - 1, 0), c) * 3;
                int down = g.index(Math.min(r + 1, g.rows() - 1), c) * 3;
                float tx = p[right] - p[left], ty = p[right + 1] - p[left + 1], tz = p[right + 2] - p[left + 2];
                float bx = p[up] - p[down], by = p[up + 1] - p[down + 1], bz = p[up + 2] - p[down + 2];
                float nx = ty * bz - tz * by;
                float ny = tz * bx - tx * bz;
                float nz = tx * by - ty * bx;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                int k = g.index(r, c) * 3;
                if (len < 1e-6F) {
                    n[k] = fallbackNx;
                    n[k + 1] = 0F;
                    n[k + 2] = fallbackNz;
                } else {
                    n[k] = nx / len;
                    n[k + 1] = ny / len;
                    n[k + 2] = nz / len;
                }
            }
        }
    }
}
