package com.bashvollink.curtaindoors.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Small vertex helpers shared by the curtain meshers. */
final class MeshUtil {
    private MeshUtil() {
    }

    static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, int argb,
                       float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /** An axis-aligned box with outward faces, the whole sprite on each face. */
    static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                    TextureAtlasSprite sprite, int argb, int light) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        // Each face: four corners counter-clockwise seen from outside.
        // -Y
        vertex(vc, pose, x0, y0, z0, argb, u0, v0, light, 0, -1, 0);
        vertex(vc, pose, x1, y0, z0, argb, u1, v0, light, 0, -1, 0);
        vertex(vc, pose, x1, y0, z1, argb, u1, v1, light, 0, -1, 0);
        vertex(vc, pose, x0, y0, z1, argb, u0, v1, light, 0, -1, 0);
        // +Y
        vertex(vc, pose, x0, y1, z0, argb, u0, v0, light, 0, 1, 0);
        vertex(vc, pose, x0, y1, z1, argb, u0, v1, light, 0, 1, 0);
        vertex(vc, pose, x1, y1, z1, argb, u1, v1, light, 0, 1, 0);
        vertex(vc, pose, x1, y1, z0, argb, u1, v0, light, 0, 1, 0);
        // -Z
        vertex(vc, pose, x0, y0, z0, argb, u0, v1, light, 0, 0, -1);
        vertex(vc, pose, x0, y1, z0, argb, u0, v0, light, 0, 0, -1);
        vertex(vc, pose, x1, y1, z0, argb, u1, v0, light, 0, 0, -1);
        vertex(vc, pose, x1, y0, z0, argb, u1, v1, light, 0, 0, -1);
        // +Z
        vertex(vc, pose, x0, y0, z1, argb, u0, v1, light, 0, 0, 1);
        vertex(vc, pose, x1, y0, z1, argb, u1, v1, light, 0, 0, 1);
        vertex(vc, pose, x1, y1, z1, argb, u1, v0, light, 0, 0, 1);
        vertex(vc, pose, x0, y1, z1, argb, u0, v0, light, 0, 0, 1);
        // -X
        vertex(vc, pose, x0, y0, z0, argb, u0, v1, light, -1, 0, 0);
        vertex(vc, pose, x0, y0, z1, argb, u1, v1, light, -1, 0, 0);
        vertex(vc, pose, x0, y1, z1, argb, u1, v0, light, -1, 0, 0);
        vertex(vc, pose, x0, y1, z0, argb, u0, v0, light, -1, 0, 0);
        // +X
        vertex(vc, pose, x1, y0, z0, argb, u0, v1, light, 1, 0, 0);
        vertex(vc, pose, x1, y1, z0, argb, u0, v0, light, 1, 0, 0);
        vertex(vc, pose, x1, y1, z1, argb, u1, v0, light, 1, 0, 0);
        vertex(vc, pose, x1, y0, z1, argb, u1, v1, light, 1, 0, 0);
    }
}
