package com.bashvollink.curtaindoors.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CommonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_LENGTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        MAX_LENGTH = b.comment("Maximum number of blocks a curtain hangs down from its ceiling.")
                .defineInRange("maxLength", 4, 1, 4);
        SPEC = b.build();
    }

    private CommonConfig() {
    }

    public static int maxLength() {
        return SPEC.isLoaded() ? MAX_LENGTH.get() : 4;
    }
}
