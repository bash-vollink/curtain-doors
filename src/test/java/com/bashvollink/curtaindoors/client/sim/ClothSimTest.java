package com.bashvollink.curtaindoors.client.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClothSimTest {
    private static final float DT = 1F / 40F;

    private static ClothSim wool(int length) {
        return new ClothSim(ClothLayout.panels(2, 5, 8, length, 0F, 0F, 0.6F, 0.15F), SimParams.WOOL);
    }

    private static void run(ClothSim sim, int ticks, List<Cylinder> colliders) {
        for (int t = 0; t < ticks; t++) {
            sim.beginTick();
            sim.step(DT, colliders, 0F, t / 20F);
            sim.step(DT, colliders, 0F, t / 20F);
        }
    }

    @Test
    void pinnedParticlesNeverMove() {
        ClothSim sim = wool(2);
        Cylinder c = new Cylinder().set(0.5F, -0.1F, 0.3F, 0F, 1.8F, 0F, 4F);
        run(sim, 40, List.of(c));
        for (int i = 0; i < sim.count; i++) {
            if (sim.isPinned(i)) {
                for (int a = 0; a < 3; a++) {
                    assertEquals(sim.layout.rest[i * 3 + a], sim.pos[i * 3 + a], 0F);
                }
            }
        }
    }

    @Test
    void settlesAfterPerturbation() {
        for (ClothLayout layout : List.of(
                ClothLayout.panels(2, 5, 8, 3, 0F, 0F, 0.6F, 0.15F),
                ClothLayout.panels(4, 3, 8, 3, 0.06F, 0.012F, 0.9F, 0.6F),
                ClothLayout.strands(8, 6, 3, 0.05F))) {
            ClothSim sim = new ClothSim(layout, SimParams.WOOL);
            for (int i = 0; i < sim.count; i++) {
                if (!sim.isPinned(i)) {
                    sim.pos[i * 3 + 2] += 0.3F * (float) Math.sin(i);
                }
            }
            run(sim, 400, List.of());
            sim.beginTick();
            sim.step(DT, List.of(), 0F, 0F);
            assertFalse(sim.hasInvalidState());
            assertTrue(sim.maxTickDisplacementSq() < 1e-6F, "cloth still moving: " + sim.maxTickDisplacementSq());
        }
    }

    @Test
    void linksStayNearRestLength() {
        ClothSim sim = wool(2);
        run(sim, 200, List.of());
        ClothLayout l = sim.layout;
        for (int c = 0; c < l.linkCount(); c++) {
            if (l.linkStiffness[c] < 1F) {
                continue;
            }
            int a = l.linkA[c] * 3, b = l.linkB[c] * 3;
            float dx = sim.pos[b] - sim.pos[a], dy = sim.pos[b + 1] - sim.pos[a + 1], dz = sim.pos[b + 2] - sim.pos[a + 2];
            float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            assertEquals(l.linkRest[c], d, l.linkRest[c] * 0.05F, "link " + c);
        }
    }

    @Test
    void entityPushesClothOutOfItsBody() {
        ClothSim sim = wool(2);
        Cylinder body = new Cylinder();
        // Walk from z = -0.6 to z = +0.6 through the middle of the curtain.
        for (int t = 0; t <= 24; t++) {
            float z = -0.6F + t * 0.05F;
            body.set(0.5F, z, 0.3F, 0F, 1.8F, 0F, 1F);
            sim.beginTick();
            sim.step(DT, List.of(body), 0F, 0F);
            sim.step(DT, List.of(body), 0F, 0F);
        }
        // Stand in the middle and let it settle: nothing may remain inside the body.
        body.set(0.5F, 0F, 0.3F, 0F, 1.8F, 0F, 0F);
        run(sim, 20, List.of(body));
        float r = 0.3F + SimParams.WOOL.colliderMargin() - 1e-3F;
        for (int i = 0; i < sim.count; i++) {
            int k = i * 3;
            float y = sim.pos[k + 1];
            if (sim.isPinned(i) || y > 1.8F) {
                continue;
            }
            float dx = sim.pos[k] - 0.5F, dz = sim.pos[k + 2];
            assertTrue(dx * dx + dz * dz >= r * r, "particle " + i + " inside body");
        }
        assertFalse(sim.hasInvalidState());
    }

    @Test
    void groupsAreNotLinkedToEachOther() {
        for (ClothLayout layout : List.of(
                ClothLayout.panels(4, 3, 8, 2, 0.06F, 0.012F, 0.9F, 0.6F),
                ClothLayout.strands(8, 6, 2, 0.05F))) {
            for (int c = 0; c < layout.linkCount(); c++) {
                assertEquals(groupOf(layout, layout.linkA[c]), groupOf(layout, layout.linkB[c]));
            }
        }
    }

    @Test
    void restPoseSpansOpening() {
        ClothLayout l = ClothLayout.panels(2, 5, 8, 3, 0F, 0F, 0.6F, 0.15F);
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < l.particleCount; i++) {
            minX = Math.min(minX, l.rest[i * 3]);
            maxX = Math.max(maxX, l.rest[i * 3]);
            minY = Math.min(minY, l.rest[i * 3 + 1]);
            maxY = Math.max(maxY, l.rest[i * 3 + 1]);
        }
        assertEquals(0F, minX, 1e-5F);
        assertEquals(1F, maxX, 1e-5F);
        assertEquals(ClothLayout.BOTTOM_GAP, minY, 1e-5F);
        assertEquals(3F - ClothLayout.TOP_INSET, maxY, 1e-5F);
    }

    private static int groupOf(ClothLayout layout, int particle) {
        for (int g = 0; g < layout.groups.size(); g++) {
            ClothLayout.Group grp = layout.groups.get(g);
            if (particle >= grp.start() && particle < grp.start() + grp.cols() * grp.rows()) {
                return g;
            }
        }
        return -1;
    }
}
