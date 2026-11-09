package com.bashvollink.curtaindoors.client.sim;

/**
 * Tuning for one curtain family. Units are blocks and seconds.
 *
 * @param gravity         downward acceleration
 * @param damping         fraction of velocity kept each substep
 * @param iterations      constraint relaxation passes per substep
 * @param shearStiffness  stiffness of diagonal links (0..1)
 * @param bendStiffness   stiffness of skip-one links that resist folding (0..1)
 * @param drag            fraction of a passing entity's velocity handed to cloth it touches
 * @param colliderMargin  extra radius around entities so cloth doesn't clip into them
 * @param windResponse    how strongly wind moves this cloth
 */
public record SimParams(float gravity, float damping, int iterations, float shearStiffness, float bendStiffness,
                        float drag, float colliderMargin, float windResponse) {

    public static final SimParams WOOL = new SimParams(9.8F, 0.970F, 10, 0.6F, 0.15F, 0.35F, 0.08F, 1.0F);
    public static final SimParams PLASTIC = new SimParams(9.8F, 0.985F, 12, 0.9F, 0.6F, 0.25F, 0.06F, 0.5F);
    public static final SimParams BEADED = new SimParams(9.8F, 0.975F, 6, 0F, 0.05F, 0.20F, 0.05F, 0.7F);
}
