package com.bashvollink.curtaindoors.registry;

import com.bashvollink.curtaindoors.CurtainDoors;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, CurtainDoors.MOD_ID);

    public static final Holder<SoundEvent> WOOL_BRUSH = register("curtain.wool.brush");
    public static final Holder<SoundEvent> PLASTIC_SWISH = register("curtain.plastic.swish");
    public static final Holder<SoundEvent> BEADS_CLACK = register("curtain.beads.clack");

    private ModSounds() {
    }

    private static Holder<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(CurtainDoors.id(name)));
    }
}
