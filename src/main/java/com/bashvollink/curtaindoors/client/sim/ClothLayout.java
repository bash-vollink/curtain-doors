package com.bashvollink.curtaindoors.client.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Rest pose and links for a curtain, in curtain-local space: x runs across the opening (0..1),
 * y runs up from the floor (0..length), z is the curtain normal. A layout is a list of
 * rectangular particle grids ("groups"). Groups are never linked to each other, which is what
 * lets wool panels part, plastic strips slide apart and bead strands swing on their own.
 */
public final class ClothLayout {
    /** Pinned row sits this far below the ceiling. */
    public static final float TOP_INSET = 0.02F;
    /** Lowest row rests this far above the floor. */
    public static final float BOTTOM_GAP = 0.04F;

    /** A rectangular grid of particles. Particle (row, col) has index start + row * cols + col; row 0 is the top. */
    public record Group(int start, int cols, int rows) {
        public int index(int row, int col) {
            return start + row * cols + col;
        }
    }

    public final int length;
    public final List<Group> groups;
    public final int particleCount;
    /** Rest positions, xyz interleaved. */
    public final float[] rest;
    public final boolean[] pinned;
    public final int[] linkA, linkB;
    public final float[] linkRest, linkStiffness;
    /**
     * Long-range tethers: for each particle, the pinned particle at the top of its column (or -1
     * if it is pinned itself) and its rest distance. Keeps tall cloth from stretching under gravity.
     */
    public final int[] tetherAnchor;
    public final float[] tetherLength;

    private ClothLayout(int length, List<Group> groups, float[] rest, boolean[] pinned, LinkList links) {
        this.length = length;
        this.groups = List.copyOf(groups);
        this.particleCount = pinned.length;
        this.rest = rest;
        this.pinned = pinned;
        this.linkA = links.a();
        this.linkB = links.b();
        this.linkRest = links.rest(rest);
        this.linkStiffness = links.stiffness();
        this.tetherAnchor = new int[particleCount];
        this.tetherLength = new float[particleCount];
        for (Group g : this.groups) {
            for (int r = 0; r < g.rows(); r++) {
                for (int c = 0; c < g.cols(); c++) {
                    int i = g.index(r, c);
                    int anchor = g.index(0, c);
                    tetherAnchor[i] = pinned[i] ? -1 : anchor;
                    float dx = rest[i * 3] - rest[anchor * 3];
                    float dy = rest[i * 3 + 1] - rest[anchor * 3 + 1];
                    float dz = rest[i * 3 + 2] - rest[anchor * 3 + 2];
                    tetherLength[i] = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                }
            }
        }
    }

    public int linkCount() {
        return linkA.length;
    }

    /**
     * Side-by-side cloth panels hanging from the top edge.
     *
     * @param panels       number of panels across the opening
     * @param cols         particle columns per panel (at least 2)
     * @param rowsPerBlock particle rows per block of height
     * @param length       curtain height in blocks
     * @param overlap      how far neighbouring panels overlap, in blocks
     * @param zStagger     z offset of every other panel so overlapping panels don't z-fight
     */
    public static ClothLayout panels(int panels, int cols, int rowsPerBlock, int length, float overlap, float zStagger,
                                     float shearStiffness, float bendStiffness) {
        if (panels < 1 || cols < 2 || rowsPerBlock < 1 || length < 1) {
            throw new IllegalArgumentException("bad panel layout");
        }
        int rows = rowsPerBlock * length + 1;
        float width = (1F + overlap * (panels - 1)) / panels;
        float top = length - TOP_INSET;
        float dy = (top - BOTTOM_GAP) / (rows - 1);
        int count = panels * cols * rows;
        float[] rest = new float[count * 3];
        boolean[] pinned = new boolean[count];
        List<Group> groups = new ArrayList<>();
        LinkList links = new LinkList();

        int start = 0;
        for (int p = 0; p < panels; p++) {
            Group g = new Group(start, cols, rows);
            groups.add(g);
            float x0 = p * (width - overlap);
            float z = (p % 2 == 0) ? 0F : zStagger;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int i = g.index(r, c);
                    rest[i * 3] = x0 + width * c / (cols - 1);
                    rest[i * 3 + 1] = top - dy * r;
                    rest[i * 3 + 2] = z;
                    pinned[i] = r == 0;
                }
            }
            linkGrid(g, links, shearStiffness, bendStiffness);
            start += cols * rows;
        }
        return new ClothLayout(length, groups, rest, pinned, links);
    }

    /**
     * Independent hanging strands, one particle per bead.
     *
     * @param strands       number of strands across the opening
     * @param beadsPerBlock beads per block of height
     */
    public static ClothLayout strands(int strands, int beadsPerBlock, int length, float bendStiffness) {
        if (strands < 1 || beadsPerBlock < 1 || length < 1) {
            throw new IllegalArgumentException("bad strand layout");
        }
        int rows = beadsPerBlock * length + 1;
        float top = length - TOP_INSET;
        float dy = (top - BOTTOM_GAP) / (rows - 1);
        int count = strands * rows;
        float[] rest = new float[count * 3];
        boolean[] pinned = new boolean[count];
        List<Group> groups = new ArrayList<>();
        LinkList links = new LinkList();
        for (int s = 0; s < strands; s++) {
            Group g = new Group(s * rows, 1, rows);
            groups.add(g);
            for (int r = 0; r < rows; r++) {
                int i = g.index(r, 0);
                rest[i * 3] = (s + 0.5F) / strands;
                rest[i * 3 + 1] = top - dy * r;
                rest[i * 3 + 2] = 0F;
                pinned[i] = r == 0;
            }
            linkGrid(g, links, 0F, bendStiffness);
        }
        return new ClothLayout(length, groups, rest, pinned, links);
    }

    private static void linkGrid(Group g, LinkList links, float shear, float bend) {
        for (int r = 0; r < g.rows(); r++) {
            for (int c = 0; c < g.cols(); c++) {
                int i = g.index(r, c);
                if (c + 1 < g.cols()) links.add(i, g.index(r, c + 1), 1F);
                if (r + 1 < g.rows()) links.add(i, g.index(r + 1, c), 1F);
                if (shear > 0 && r + 1 < g.rows() && c + 1 < g.cols()) {
                    links.add(i, g.index(r + 1, c + 1), shear);
                    links.add(g.index(r, c + 1), g.index(r + 1, c), shear);
                }
                if (bend > 0) {
                    if (c + 2 < g.cols()) links.add(i, g.index(r, c + 2), bend);
                    if (r + 2 < g.rows()) links.add(i, g.index(r + 2, c), bend);
                }
            }
        }
    }

    /** Growable link storage used while building a layout. */
    private static final class LinkList {
        private int[] a = new int[64], b = new int[64];
        private float[] s = new float[64];
        private int size;

        void add(int i, int j, float stiffness) {
            if (size == a.length) {
                a = java.util.Arrays.copyOf(a, size * 2);
                b = java.util.Arrays.copyOf(b, size * 2);
                s = java.util.Arrays.copyOf(s, size * 2);
            }
            a[size] = i;
            b[size] = j;
            s[size] = stiffness;
            size++;
        }

        int[] a() { return java.util.Arrays.copyOf(a, size); }
        int[] b() { return java.util.Arrays.copyOf(b, size); }
        float[] stiffness() { return java.util.Arrays.copyOf(s, size); }

        float[] rest(float[] pos) {
            float[] out = new float[size];
            for (int k = 0; k < size; k++) {
                float dx = pos[b[k] * 3] - pos[a[k] * 3];
                float dy = pos[b[k] * 3 + 1] - pos[a[k] * 3 + 1];
                float dz = pos[b[k] * 3 + 2] - pos[a[k] * 3 + 2];
                out[k] = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            return out;
        }
    }
}
