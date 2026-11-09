package com.bashvollink.curtaindoors.registry;

import com.bashvollink.curtaindoors.CurtainDoors;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CurtainDoors.MOD_ID);

    public static final Supplier<CreativeModeTab> CURTAINS = TABS.register("curtains", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + CurtainDoors.MOD_ID))
            .icon(() -> new ItemStack(ModBlocks.WOOL_CURTAINS.get(DyeColor.RED).get()))
            .displayItems((params, output) -> ModItems.ALL.forEach(item -> output.accept(item.get())))
            .build());

    private ModCreativeTabs() {
    }
}
