package com.bashvollink.curtaindoors.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public enum Quality { LOW, MEDIUM, HIGH }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SIM_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_ACTIVE_SIMS;
    public static final ModConfigSpec.EnumValue<Quality> QUALITY;
    public static final ModConfigSpec.BooleanValue WIND;
    public static final ModConfigSpec.BooleanValue CONTACT_SOUNDS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SIM_DISTANCE = b.comment("Curtains farther than this many blocks from the camera stop simulating and hang at rest.")
                .defineInRange("simDistance", 32, 4, 256);
        MAX_ACTIVE_SIMS = b.comment("Maximum number of curtains simulated at once; the nearest ones win.")
                .defineInRange("maxActiveSims", 64, 1, 1024);
        QUALITY = b.comment("Cloth particle density. Changing it rebuilds curtains in view.")
                .defineEnum("quality", Quality.MEDIUM);
        WIND = b.comment("Gentle wind on curtains that are open to the sky.")
                .define("wind", true);
        CONTACT_SOUNDS = b.comment("Play a soft sound when something brushes through a curtain.")
                .define("contactSounds", true);
        SPEC = b.build();
    }

    private ClientConfig() {
    }

    public static int simDistance() { return SPEC.isLoaded() ? SIM_DISTANCE.get() : 32; }
    public static int maxActiveSims() { return SPEC.isLoaded() ? MAX_ACTIVE_SIMS.get() : 64; }
    public static Quality quality() { return SPEC.isLoaded() ? QUALITY.get() : Quality.MEDIUM; }
    public static boolean wind() { return !SPEC.isLoaded() || WIND.get(); }
    public static boolean contactSounds() { return !SPEC.isLoaded() || CONTACT_SOUNDS.get(); }
}
