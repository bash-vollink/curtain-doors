package com.bashvollink.curtaindoors.client.render;

import com.bashvollink.curtaindoors.client.sim.ClothLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Draws bead strands: a small cube per bead joined by a thin string. */
final class BeadMesher {
    private static final float BEAD_HALF = 0.03F;
    private static final float STRING_HALF = 0.006F;
    private static final int STRING_COLOR = 0xFF2A2420;

    private BeadMesher() {
    }

    static void render(VertexConsumer vc, PoseStack.Pose pose, ClothLayout layout, float[] p, int[] lights,
                       float yToLocal, TextureAtlasSprite beadSprite, TextureAtlasSprite stringSprite, int beadArgb) {
        for (ClothLayout.Group g : layout.groups) {
            for (int r = 0; r < g.rows(); r++) {
                int k = g.index(r, 0) * 3;
                float x = p[k], y = p[k + 1], z = p[k + 2];
                int light = light(lights, y + yToLocal);
                if (r > 0) {
                    MeshUtil.box(vc, pose, x - BEAD_HALF, y - BEAD_HALF, z - BEAD_HALF,
                            x + BEAD_HALF, y + BEAD_HALF, z + BEAD_HALF, beadSprite, beadArgb, light);
                    int j = g.index(r - 1, 0) * 3;
                    string(vc, pose, p[j], p[j + 1], p[j + 2], x, y, z, stringSprite, light);
                }
            }
        }
    }

    private static int light(int[] lights, float localY) {
        int block = (int) Math.floor(localY);
        return lights[Math.max(0, Math.min(lights.length - 1, block))];
    }

    /** Two crossed thin ribbons between two points, both sides each. */
    private static void string(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0,
                               float x1, float y1, float z1, TextureAtlasSprite sprite, int light) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        for (int axis = 0; axis < 2; axis++) {
            float ox = axis == 0 ? STRING_HALF : 0F;
            float oz = axis == 0 ? 0F : STRING_HALF;
            float nx = axis == 0 ? 0F : -1F; // matches winding: offset along z faces -x
            float nz = axis == 0 ? 1F : 0F;
            for (int side = -1; side <= 1; side += 2) {
                float sx = ox * side, sz = oz * side;
                MeshUtil.vertex(vc, pose, x1 - sx, y1, z1 - sz, STRING_COLOR, u0, v1, light, nx * side, 0, nz * side);
                MeshUtil.vertex(vc, pose, x1 + sx, y1, z1 + sz, STRING_COLOR, u1, v1, light, nx * side, 0, nz * side);
                MeshUtil.vertex(vc, pose, x0 + sx, y0, z0 + sz, STRING_COLOR, u1, v0, light, nx * side, 0, nz * side);
                MeshUtil.vertex(vc, pose, x0 - sx, y0, z0 - sz, STRING_COLOR, u0, v0, light, nx * side, 0, nz * side);
            }
        }
    }
}
